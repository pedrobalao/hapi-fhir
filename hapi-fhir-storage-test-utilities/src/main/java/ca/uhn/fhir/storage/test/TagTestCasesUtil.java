package ca.uhn.fhir.storage.test;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

import static ca.uhn.fhir.test.utilities.TagTestUtil.assertCodingsEqualAndInOrder;
import static ca.uhn.fhir.test.utilities.TagTestUtil.createMeta;
import static ca.uhn.fhir.test.utilities.TagTestUtil.generateAllCodingPairs;
import static ca.uhn.fhir.test.utilities.TagTestUtil.toStringList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contains some test case helper functions for testing the storage of meta properties: tag, security and profile
 */
public class TagTestCasesUtil {

	private IFhirResourceDao<Patient> myPatientDao;

	private RequestDetails myRequestDetails;

	public TagTestCasesUtil(IFhirResourceDao<Patient> thePatientDao, RequestDetails theRequestDetails) {
		this.myPatientDao = thePatientDao;
		this.myRequestDetails = theRequestDetails;
	}

	/**
	 * Creates a resource with the given Meta and reads the resource back and asserts that the resource
	 * has the specified meta properties for tag, security and profile
	 * @param theMetaInputOnCreate
	 * @param theExpectedMetaAfterCreate
	 * @return created resource
	 */
	public IBaseResource createResourceAndVerifyMeta (Meta theMetaInputOnCreate, Meta theExpectedMetaAfterCreate) {
		DaoMethodOutcome createOutcome = createPatient(theMetaInputOnCreate);

		IIdType versionlessPatientId = createOutcome.getResource().getIdElement().toVersionless();
		Patient createdPatient = myPatientDao.read(versionlessPatientId, myRequestDetails);

		assertCodingsEqualAndInOrder(theExpectedMetaAfterCreate.getTag(), createdPatient.getMeta().getTag());
		assertCodingsEqualAndInOrder(theExpectedMetaAfterCreate.getSecurity(), createdPatient.getMeta().getSecurity());
		assertEquals(toStringList(theExpectedMetaAfterCreate.getProfile()), toStringList(createdPatient.getMeta().getProfile()));
		return createdPatient;
	}

	/**
	 * Creates a resource with the given meta properties, then updates the resource with the specified meta properties, then
	 * reads the resource back and asserts that the resource has the specified properties for tag, security and profile
	 * @param theMetaInputOnCreate
	 * @param theMetaInputOnUpdate
	 * @param theExpectedMetaAfterUpdate
	 * @param theExpectNop
	 * @return the resource
	 */
	public IBaseResource updateResourceAndVerifyMeta(Meta theMetaInputOnCreate, Meta theMetaInputOnUpdate, Meta theExpectedMetaAfterUpdate, boolean theExpectNop) {
		DaoMethodOutcome createOutcome = createPatient(theMetaInputOnCreate);
		IIdType versionlessPatientId = createOutcome.getId().toVersionless();

		DaoMethodOutcome updateOutcome = updatePatient(versionlessPatientId, theMetaInputOnUpdate);
		assertEquals(theExpectNop, updateOutcome.isNop());

		Patient patient = myPatientDao.read(versionlessPatientId, myRequestDetails);

		assertCodingsEqualAndInOrder(theExpectedMetaAfterUpdate.getTag(), patient.getMeta().getTag());
		assertCodingsEqualAndInOrder(theExpectedMetaAfterUpdate.getSecurity(), patient.getMeta().getSecurity());
		assertEquals(toStringList(theExpectedMetaAfterUpdate.getProfile()), toStringList(patient.getMeta().getProfile()));

		return patient;
	}

	/**
	 * creates a resource with some meta properties, then updates the resource with the same meta properties in different order
	 * and asserts that the resource version remains the same after the update
	 */
	public void updateResourceWithExistingTagsButInDifferentOrderAndExpectVersionToRemainTheSame(){

		Meta metaInputOnCreate = createMeta(
			// generateAllCodingPairs creates a list that has 6 codings in this case in this order:
			// (sys2, c), (sys2, b), (sys2, a), (sys1, c), (sys1, b), (sys1, a)
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

		updatePatient(versionlessPatientId, metaInputOnUpdate);

		// updating the resource with the same set of tags shouldn't cause a new version to be created
		Patient patient = myPatientDao.read(versionlessPatientId, myRequestDetails);
		assertEquals("1", patient.getMeta().getVersionId());

	}

	public void createResourceWithTagsAndExpectToRetrieveThemSorted() {

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);

		//expect properties to be alphabetically sorted
		Meta expectedMetaAfterCreate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //tag (sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "b", "c")), //security (sorted)
			List.of("a", "b", "c") //profile (sorted)
		);

		createResourceAndVerifyMeta(metaInputOnCreate, expectedMetaAfterCreate);
	}


	public void updateResourceWithTagsAndExpectNonInlineModeUpdateBehaviourAndExpectToRetrieveTagsSorted() {
		// meta input for initial creation
		Meta metaInputOnCreate = createMeta(
			// generateAllCodingPairs creates a list that has 6 codings in this case in this order:
			// (sys2, c), (sys2, b), (sys2, a), (sys1, c), (sys1, b), (sys1, a)
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

		// the new tags & security must be added to the existing set and must be in alphabetical order
		// the profile will be completely replaced
		Meta expectedMetaAfterUpdate = createMeta(
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //tag (added & sorted)
			generateAllCodingPairs(List.of("sys1", "sys2"), List.of("a", "aa", "b", "bb", "c", "cc")), //security (added & sorted)
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
