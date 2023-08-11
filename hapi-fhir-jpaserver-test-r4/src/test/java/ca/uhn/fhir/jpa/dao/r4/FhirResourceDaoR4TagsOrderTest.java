package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.test.BaseJpaR4Test;
import ca.uhn.fhir.storage.TagOrderInterceptor;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FhirResourceDaoR4TagsOrderTest extends BaseJpaR4Test {



	private TagOrderInterceptor myTagOrderInterceptor;

	@Override
	@BeforeEach
	protected void before() throws Exception {
		super.before();
		myTagOrderInterceptor = new TagOrderInterceptor();
		myInterceptorRegistry.registerInterceptor(myTagOrderInterceptor);
	}


	@AfterEach
	protected void after() throws Exception {
		myInterceptorRegistry.unregisterInterceptor(myTagOrderInterceptor);
	}

	@ParameterizedTest
	@EnumSource(JpaStorageSettings.TagStorageModeEnum.class)
	public void testTagsOrderedOnCreate(JpaStorageSettings.TagStorageModeEnum tagStorageMode) {
		myStorageSettings.setTagStorageMode(tagStorageMode);

		DaoMethodOutcome createOutcome = createPatientWithUnorderedInitialTags();

		Patient createdPatient = myPatientDao.read(createOutcome.getResource().getIdElement(), mySrd);

		List<Coding> expectedCodingList = List.of(
			createCoding("sys1", "A"),
			createCoding("sys1", "B"),
			createCoding("sys1", "C"),
			createCoding("sys2", "A"),
			createCoding("sys2", "B"),
			createCoding("sys2", "C")
		);

		verifyCodingsInOrder(expectedCodingList, createdPatient.getMeta().getTag());
		verifyCodingsInOrder(expectedCodingList, createdPatient.getMeta().getSecurity());
		assertEquals(List.of("A", "B", "C"), toStringList(createdPatient.getMeta().getProfile()));
	}
	@ParameterizedTest
	@EnumSource(JpaStorageSettings.TagStorageModeEnum.class)
	public void testUpdateShouldNotIncreaseVersionBecauseOfTagOrder(JpaStorageSettings.TagStorageModeEnum tagStorageMode) {
		myStorageSettings.setTagStorageMode(tagStorageMode);

		DaoMethodOutcome createOutcome = createPatientWithUnorderedInitialTags();

		DaoMethodOutcome updateOutcome = updatePatientWithUnorderedInitialTags(createOutcome.getResource().getIdElement());

		assertTrue(updateOutcome.isNop());

		Patient patient = myPatientDao.read(createOutcome.getResource().getIdElement(), mySrd);
		assertEquals("1", patient.getMeta().getVersionId());
	}

	private DaoMethodOutcome updatePatientWithUnorderedInitialTags(IIdType patientId) {
		Meta meta = constructTestMetaWithUnorderedInitialTags();
		Patient inputPatient = new Patient();
		inputPatient.setId(patientId);
		inputPatient.setMeta(meta);

		return myPatientDao.update(inputPatient, mySrd);
	}

	private DaoMethodOutcome createPatientWithUnorderedInitialTags() {
		Meta meta = constructTestMetaWithUnorderedInitialTags();
		Patient inputPatient = new Patient();
		inputPatient.setId("p");
		inputPatient.setMeta(meta);

		return myPatientDao.create(inputPatient, mySrd);
	}

	private Meta constructTestMetaWithUnorderedInitialTags( ) {

		List<Coding> tags = List.of(
			createCoding("sys2", "C"),
			createCoding("sys2", "B"),
			createCoding("sys2", "A"),
			createCoding("sys1", "C"),
			createCoding("sys1", "B"),
			createCoding("sys1", "A")
		);

		List<Coding>  securityTags = List.of(
			createCoding("sys2", "C"),
			createCoding("sys2", "B"),
			createCoding("sys2", "A"),
			createCoding("sys1", "C"),
			createCoding("sys1", "B"),
			createCoding("sys1", "A")
		);

		List<String> profiles = List.of("C", "B", "A");

		return createMeta(tags, securityTags, profiles);
	}

	private void verifyCodingsInOrder(List<Coding> expectedCodings, List<Coding> actualCodings) {
		assertEquals(expectedCodings.size(), actualCodings.size());
		for (int i = 0; i < expectedCodings.size(); i++) {
			assertEquals(expectedCodings.get(i).getSystem(), actualCodings.get(i).getSystem());
			assertEquals(expectedCodings.get(i).getCode(), actualCodings.get(i).getCode());
		}
	}


	private List<String> toStringList(List<CanonicalType> canonicalTypes) {
		return canonicalTypes.stream().map(c -> c.getValue()).collect(Collectors.toList());
	}

	private Coding createCoding(String system, String code) {
		return new Coding().setSystem(system).setCode(code);
	}

	private Meta createMeta(List<Coding> tags, List<Coding> securityLabels, List<String> profiles){
		Meta meta = new Meta();
		meta.setTag(new ArrayList<>(tags));
		meta.setSecurity(new ArrayList<>(securityLabels));
		meta.setProfile(profiles.stream().map(p -> new CanonicalType(p)).collect(Collectors.toList()));
		return meta;
	}

}
