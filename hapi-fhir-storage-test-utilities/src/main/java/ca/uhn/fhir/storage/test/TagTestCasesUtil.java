package ca.uhn.fhir.storage.test;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

import static ca.uhn.fhir.test.utilities.TagTestUtil.assertCodingsEqual;
import static ca.uhn.fhir.test.utilities.TagTestUtil.createMeta;
import static ca.uhn.fhir.test.utilities.TagTestUtil.generateAllCodingPairs;
import static ca.uhn.fhir.test.utilities.TagTestUtil.toStringList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class TagTestCasesUtil {

	private IFhirResourceDao<Patient> myPatientDao;

	private RequestDetails myRequestDetails;

	public TagTestCasesUtil(IFhirResourceDao<Patient> thePatientDao, RequestDetails theRequestDetails) {
		this.myPatientDao = thePatientDao;
		this.myRequestDetails = theRequestDetails;
	}

	public IBaseResource createResourceAndVerifyMeta (Meta metaInputOnCreate, Meta expectedMetaAfterCreate) {
		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);

		IIdType patientId = createOutcome.getResource().getIdElement().toVersionless();
		Patient createdPatient = myPatientDao.read(patientId, myRequestDetails);

		assertCodingsEqual(expectedMetaAfterCreate.getTag(), createdPatient.getMeta().getTag());
		assertCodingsEqual(expectedMetaAfterCreate.getSecurity(), createdPatient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterCreate.getProfile()), toStringList(createdPatient.getMeta().getProfile()));

		return createdPatient;
	}


	public IBaseResource updateResourceAndVerifyMeta(Meta metaInputOnCreate, Meta metaInputOnUpdate, Meta expectedMetaAfterUpdate, boolean expectNop) {
		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);
		IIdType versionlessPatientId = createOutcome.getId().toVersionless();

		DaoMethodOutcome updateOutcome = updatePatient(versionlessPatientId, metaInputOnUpdate);
		assertEquals(expectNop, updateOutcome.isNop());

		Patient patient = myPatientDao.read(versionlessPatientId, myRequestDetails);

		assertCodingsEqual(expectedMetaAfterUpdate.getTag(), patient.getMeta().getTag());
		assertCodingsEqual(expectedMetaAfterUpdate.getSecurity(), patient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterUpdate.getProfile()), toStringList(patient.getMeta().getProfile()));


		// FIX ME: The meta endpoint isn't calling PRESHOW, so ordering is not working
/*		Meta meta = myPatientDao.metaGetOperation(Meta.class, patientId, mySrd);
		verifyCodingsInOrder(expectedCodingList, meta.getTag());
		verifyCodingsInOrder(expectedCodingList, meta.getSecurity());
		assertEquals(List.of("A", "B", "C"), toStringList(meta.getProfile()));*/

		return patient;


	}



	public void updateResourceWithExistingTagsButInDifferentOrderAndExpectVersionToRemainTheSame(){

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);

		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);
		IIdType versionlessPatientId = createOutcome.getId().toVersionless();

		// use the same input on update as the creation but order everything differently
		Meta metaInputOnUpdate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("b", "c", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("b", "c", "a")), //security
			List.of("b", "c", "a") // profile
		);


		// updating the resource with the same set of tags shouldn't cause a new version to be created
		updatePatient(versionlessPatientId, metaInputOnUpdate);

		Patient patient = myPatientDao.read(versionlessPatientId, myRequestDetails);
		assertEquals("1", patient.getMeta().getVersionId());

	}

	public void createResourceWithTagsAndExpectToRetrieveThemSorted() {

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);


		Meta expectedMetaAfterCreate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //tag (sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //security (sorted)
			List.of("a", "b", "c") //profile (sorted)
		);


		createResourceAndVerifyMeta(metaInputOnCreate, expectedMetaAfterCreate);



		// FIX ME: The meta endpoint isn't calling PRESHOW, so ordering is not working
		// TODO: $meta operation doesn't return tags when using INLINE storage mode https://github.com/hapifhir/hapi-fhir/issues/5206
/*		Meta meta = myPatientDao.metaGetOperation(Meta.class, patientId, mySrd);
		verifyCodingsInOrder(expectedCodingList, meta.getTag());
		verifyCodingsInOrder(expectedCodingList, meta.getSecurity());
		assertEquals(List.of("A", "B", "C"), toStringList(meta.getProfile()));*/
	}


	public void updateResourceWithTagsAndExpectNonInlineModeUpdateBehaviourAndExpectToRetrieveTagsSorted() {
		// meta input for initial creation
		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
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
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //tag (merged & sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //security (merged & sorted)
			List.of("aa", "bb", "cc") //profile (replaced & sorted)
		);

		IBaseResource resource = updateResourceAndVerifyMeta(metaInputOnCreate,  metaInputOnUpdate, expectedMetaAfterUpdate, false);
		// expect the resource version to be 2, since the meta is updated
		assertEquals("2", resource.getMeta().getVersionId());

	}
	private DaoMethodOutcome createPatient(Meta theMeta) {
		Patient inputPatient = new Patient();
		inputPatient.setMeta(theMeta);
		return myPatientDao.create(inputPatient, myRequestDetails);
	}

	private DaoMethodOutcome updatePatient(IIdType thePatientId, Meta theMeta) {
		Patient inputPatient = new Patient();
		inputPatient.setId(thePatientId);
		inputPatient.setMeta(theMeta);

		return myPatientDao.update(inputPatient, myRequestDetails);
	}
}
