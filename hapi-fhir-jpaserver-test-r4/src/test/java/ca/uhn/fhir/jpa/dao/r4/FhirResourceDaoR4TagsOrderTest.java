package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.test.BaseJpaR4Test;
import ca.uhn.fhir.storage.TagOrderInterceptor;
import graphql.Assert;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/*	@Test
	public void testWhenResourceCreated_TagsOrderedOnRead() {
		JpaStorageSettings.TagStorageModeEnum tagStorageMode = JpaStorageSettings.TagStorageModeEnum.INLINE;*/
	@ParameterizedTest
	@EnumSource(JpaStorageSettings.TagStorageModeEnum.class)
	public void testWhenResourceCreated_TagsOrderedOnRead(JpaStorageSettings.TagStorageModeEnum tagStorageMode) {
		myStorageSettings.setTagStorageMode(tagStorageMode);

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);


		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);

		IIdType patientId = createOutcome.getResource().getIdElement();
		Patient createdPatient = myPatientDao.read(patientId, mySrd);


		Meta expectedMetaAfterCreate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //tag (sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //security (sorted)
			List.of("a", "b", "c") //profile (sorted)
		);


		verifyCodingsInOrder(expectedMetaAfterCreate.getTag(), createdPatient.getMeta().getTag());
		verifyCodingsInOrder(expectedMetaAfterCreate.getSecurity(), createdPatient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterCreate.getProfile()), toStringList(createdPatient.getMeta().getProfile()));


		// FIX ME: The meta endpoint isn't calling PRESHOW, so ordering is not working
		// TODO: $meta operation doesn't return tags when using INLINE storage mode https://github.com/hapifhir/hapi-fhir/issues/5206
/*		Meta meta = myPatientDao.metaGetOperation(Meta.class, patientId, mySrd);
		verifyCodingsInOrder(expectedCodingList, meta.getTag());
		verifyCodingsInOrder(expectedCodingList, meta.getSecurity());
		assertEquals(List.of("A", "B", "C"), toStringList(meta.getProfile()));*/

	}

	@ParameterizedTest
	@EnumSource(JpaStorageSettings.TagStorageModeEnum.class)
	public void testUpdateShouldNotIncreaseVersionBecauseOfTagOrder(JpaStorageSettings.TagStorageModeEnum tagStorageMode) {
		myStorageSettings.setTagStorageMode(tagStorageMode);

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);


		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);
		IIdType patientId = createOutcome.getId();

		// use the same input on update as the create
		Meta metaInputOnUpdate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);
		// updating the resource with the same set of tags shouldn't cause a new version to be created
		DaoMethodOutcome updateOutcome = updatePatient(patientId, metaInputOnUpdate);

		assertTrue(updateOutcome.isNop());

		Patient patient = myPatientDao.read(createOutcome.getResource().getIdElement(), mySrd);
		assertEquals("1", patient.getMeta().getVersionId());

	}



	@ParameterizedTest
	@EnumSource(
		value = JpaStorageSettings.TagStorageModeEnum.class,
		names = {"INLINE"},
		mode = EnumSource.Mode.EXCLUDE)
	public void testWhenResourceUpdate_TagsOrderedOnRead_NonInlineModes(JpaStorageSettings.TagStorageModeEnum tagStorageMode) {
		myStorageSettings.setTagStorageMode(tagStorageMode);
			// meta input for initial create
		Meta metaInputOnCreate = createMeta(
				generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
				generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
				List.of("c", "b", "a") // profile
			);
			//meta expected on read after create (everything must be sorted):
			Meta expectedMetaAfterCreate =  createMeta(
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //tag (sorted)
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //security (sorted)
				List.of("a", "b", "c") //profile (sorted)
			);
			// meta input for update (adding new tags)
		Meta metaInputOnUpdate = createMeta(
				generateAllCodingPairs(List.of("sys2", "sys1"), List.of("cc", "bb", "aa")), //tag
				generateAllCodingPairs(List.of("sys2", "sys1"), List.of("cc", "bb", "aa")), //security
				List.of("cc", "bb", "aa") //profile
			);
			// the new tags must be added to the existing set and must appear in the right order
		Meta expectedMetaAfterUpdate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //tag (sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //security (sorted)
			List.of("aa", "bb", "cc") //profile (replaced and sorted)
			);

		testCreateAndUpdate(metaInputOnCreate, expectedMetaAfterCreate, metaInputOnUpdate, expectedMetaAfterUpdate);

	}



	@Test
	public void testWhenResourceUpdate_TagsOrderedOnRead_InlineMode() {
		myStorageSettings.setTagStorageMode(JpaStorageSettings.TagStorageModeEnum.INLINE);

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);
		//meta expected on read after create (everything must be sorted):
		Meta expectedMetaAfterCreate =  createMeta(
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //tag (sorted)
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //security (sorted)
				List.of("a", "b", "c") //profile (sorted)
			);

		// meta input for update (adding new tags)
		Meta metaInputOnUpdate = createMeta(
				generateAllCodingPairs(List.of("sys2", "sys1"), List.of("cc", "bb", "aa")), //tag
				generateAllCodingPairs(List.of("sys2", "sys1"), List.of("cc", "bb", "aa")), //security
				List.of("cc", "bb", "aa") //profile
			);

		// the new tags must be added to the existing set and must appear in the right order
		Meta expectedMetaAfterUpdate = createMeta(
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("aa", "bb", "cc")), //tag (sorted)
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("aa", "bb", "cc")), //security (sorted)
				List.of("aa", "bb", "cc") //profile (sorted)
			);

		testCreateAndUpdate(metaInputOnCreate, expectedMetaAfterCreate, metaInputOnUpdate, expectedMetaAfterUpdate);

	}

		public void testCreateAndUpdate(Meta metaInputOnCreate, Meta expectedMetaAfterCreate, Meta metaInputOnUpdate, Meta expectedMetaAfterUpdate) {

		IIdType patientId = new IdType().setValue("patientId1");
		DaoMethodOutcome updateOutcome = updatePatient(patientId, metaInputOnCreate);
		Assert.assertFalse(updateOutcome.isNop());

		Patient patient = myPatientDao.read(patientId, mySrd);
		assertEquals("1", patient.getMeta().getVersionId());

		verifyCodingsInOrder(expectedMetaAfterCreate.getTag(), patient.getMeta().getTag());
		verifyCodingsInOrder(expectedMetaAfterCreate.getSecurity(), patient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterCreate.getProfile()), toStringList(patient.getMeta().getProfile()));

		updateOutcome = updatePatient(patientId, metaInputOnUpdate);
		Assert.assertFalse(updateOutcome.isNop());

		patient = myPatientDao.read(patientId, mySrd);
		assertEquals("2", patient.getMeta().getVersionId());

		verifyCodingsInOrder(expectedMetaAfterUpdate.getTag(), patient.getMeta().getTag());
		verifyCodingsInOrder(expectedMetaAfterUpdate.getSecurity(), patient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterUpdate.getProfile()), toStringList(patient.getMeta().getProfile()));

		// FIX ME: The meta endpoint isn't calling PRESHOW, so ordering is not working
/*		Meta meta = myPatientDao.metaGetOperation(Meta.class, patientId, mySrd);
		verifyCodingsInOrder(expectedCodingList, meta.getTag());
		verifyCodingsInOrder(expectedCodingList, meta.getSecurity());
		assertEquals(List.of("A", "B", "C"), toStringList(meta.getProfile()));*/

	}

	private DaoMethodOutcome updatePatient(IIdType patientId, Meta meta) {
		Patient inputPatient = new Patient();
		inputPatient.setId(patientId);
		inputPatient.setMeta(meta);

		return myPatientDao.update(inputPatient, mySrd);
	}


	private DaoMethodOutcome createPatient(Meta meta) {
		Patient inputPatient = new Patient();
		inputPatient.setMeta(meta);
		return myPatientDao.create(inputPatient, mySrd);
	}



	private static List<Coding> generateAllCodingPairs(List<String> systems, List<String> codes) {
		List<Coding> result = new ArrayList<>();
		for (String system: systems) {
			for (String code: codes) {
				result.add(createCoding(system, code));
			}
		}
		return result;
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

	private static Coding createCoding(String system, String code) {
		return new Coding().setSystem(system).setCode(code);
	}

	private static Meta createMeta(List<Coding> tags, List<Coding> securityLabels, List<String> profiles) {
		Meta meta = new Meta();
		meta.setTag(new ArrayList<>(tags));
		meta.setSecurity(new ArrayList<>(securityLabels));
		meta.setProfile(profiles.stream().map(p -> new CanonicalType(p)).collect(Collectors.toList()));
		return meta;
	}

}
