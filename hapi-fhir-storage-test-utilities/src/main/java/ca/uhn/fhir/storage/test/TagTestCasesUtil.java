package ca.uhn.fhir.storage.test;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
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

	public Patient createResourceAndVerifyMeta( Meta metaInputOnCreate, Meta expectedMetaAfterCreate) {
		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);

		IIdType patientId = createOutcome.getResource().getIdElement().toVersionless();
		Patient createdPatient = myPatientDao.read(patientId, myRequestDetails);

		assertCodingsEqual(expectedMetaAfterCreate.getTag(), createdPatient.getMeta().getTag());
		assertCodingsEqual(expectedMetaAfterCreate.getSecurity(), createdPatient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterCreate.getProfile()), toStringList(createdPatient.getMeta().getProfile()));

		return createdPatient;
	}

	public void updateResourceAndVerifyMeta(Meta metaInputOnCreate, Meta expectedMetaAfterCreate, Meta metaInputOnUpdate, Meta expectedMetaAfterUpdate) {

		IIdType patientId = new IdType().setValue("patientId1");
		DaoMethodOutcome updateOutcome = updatePatient(patientId, metaInputOnCreate);
		assertFalse(updateOutcome.isNop());

		Patient patient = myPatientDao.read(patientId, myRequestDetails);
		assertEquals("1", patient.getMeta().getVersionId());

		assertCodingsEqual(expectedMetaAfterCreate.getTag(), patient.getMeta().getTag());
		assertCodingsEqual(expectedMetaAfterCreate.getSecurity(), patient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterCreate.getProfile()), toStringList(patient.getMeta().getProfile()));

		updateOutcome = updatePatient(patientId, metaInputOnUpdate);
		assertFalse(updateOutcome.isNop());

		patient = myPatientDao.read(patientId, myRequestDetails);
		assertEquals("2", patient.getMeta().getVersionId());

		assertCodingsEqual(expectedMetaAfterUpdate.getTag(), patient.getMeta().getTag());
		assertCodingsEqual(expectedMetaAfterUpdate.getSecurity(), patient.getMeta().getSecurity());
		assertEquals(toStringList(expectedMetaAfterUpdate.getProfile()), toStringList(patient.getMeta().getProfile()));

		// FIX ME: The meta endpoint isn't calling PRESHOW, so ordering is not working
/*		Meta meta = myPatientDao.metaGetOperation(Meta.class, patientId, mySrd);
		verifyCodingsInOrder(expectedCodingList, meta.getTag());
		verifyCodingsInOrder(expectedCodingList, meta.getSecurity());
		assertEquals(List.of("A", "B", "C"), toStringList(meta.getProfile()));*/

	}



	public void testResourceUpdateDoesNotCreateNewVersionBecauseOfTagOrder(){

		Meta metaInputOnCreate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("c", "b", "a")), //security
			List.of("c", "b", "a") // profile
		);

		DaoMethodOutcome createOutcome = createPatient(metaInputOnCreate);
		IIdType versionlessPatientId = createOutcome.getId().toVersionless();

		// use the same input on update as the creation but order differently
		Meta metaInputOnUpdate = createMeta(
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("b", "c", "a")), //tag
			generateAllCodingPairs(List.of("sys2", "sys1"), List.of("b", "c", "a")), //security
			List.of("b", "c", "a") // profile
		);

		// updating the resource with the same set of tags shouldn't cause a new version to be created
		DaoMethodOutcome updateOutcome = updatePatient(versionlessPatientId, metaInputOnUpdate);

		Patient patient = myPatientDao.read(versionlessPatientId, myRequestDetails);
		assertEquals("1", patient.getMeta().getVersionId());

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
