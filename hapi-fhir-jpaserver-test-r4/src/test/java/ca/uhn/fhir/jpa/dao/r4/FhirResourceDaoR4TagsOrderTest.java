package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.test.BaseJpaR4Test;
import ca.uhn.fhir.storage.TagOrderInterceptor;
import ca.uhn.fhir.storage.test.TagTestCasesUtil;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static ca.uhn.fhir.test.utilities.TagTestUtil.createMeta;
import static ca.uhn.fhir.test.utilities.TagTestUtil.generateAllCodingPairs;
import static ca.uhn.fhir.test.utilities.TagTestUtil.toStringList;
import static ca.uhn.fhir.test.utilities.TagTestUtil.assertCodingsEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FhirResourceDaoR4TagsOrderTest extends BaseJpaR4Test {


	private TagOrderInterceptor myTagOrderInterceptor;

	private TagTestCasesUtil myTagTestCasesUtil;

	@Override
	@BeforeEach
	protected void before() throws Exception {
		super.before();
		myTagOrderInterceptor = new TagOrderInterceptor();
		myInterceptorRegistry.registerInterceptor(myTagOrderInterceptor);
		myTagTestCasesUtil = new TagTestCasesUtil(myPatientDao, mySrd);
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

/*
		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);

		IIdType patientId = createOutcome.getResource().getIdElement();
		Patient createdPatient = myPatientDao.read(patientId, mySrd);*/


		Meta expectedMetaAfterCreate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //tag (sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //security (sorted)
			List.of("a", "b", "c") //profile (sorted)
		);


		myTagTestCasesUtil.createResourceAndVerifyMeta(metaInputOnCreate, expectedMetaAfterCreate);


/*		assertCodingsEqual(expectedMetaAfterCreate.getTag(), createdPatient.getMeta().getTag());
		assertCodingsEqual(expectedMetaAfterCreate.getSecurity(), createdPatient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterCreate.getProfile()), toStringList(createdPatient.getMeta().getProfile()));*/


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

		myTagTestCasesUtil.testResourceUpdateDoesNotCreateNewVersionBecauseOfTagOrder();


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
		// the new tags&security must be added to the existing set and must appear in the right order
		// the profile will be completely replaced
		Meta expectedMetaAfterUpdate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //tag (sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //security (sorted)
			List.of("aa", "bb", "cc") //profile (replaced and sorted)
			);

		myTagTestCasesUtil.updateResourceAndVerifyMeta(metaInputOnCreate, expectedMetaAfterCreate, metaInputOnUpdate, expectedMetaAfterUpdate);
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

		// inline mode replaces the tags completely on update, so only new tags are expected after update
		Meta expectedMetaAfterUpdate = createMeta(
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("aa", "bb", "cc")), //tag (sorted)
				generateAllCodingPairs(List.of("sys1", "sys2"), List.of("aa", "bb", "cc")), //security (sorted)
				List.of("aa", "bb", "cc") //profile (sorted)
			);

		myTagTestCasesUtil.updateResourceAndVerifyMeta(metaInputOnCreate, expectedMetaAfterCreate, metaInputOnUpdate, expectedMetaAfterUpdate);

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


}
