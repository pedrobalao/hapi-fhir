package ca.uhn.fhir.jpa.subscription.websocket;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import ca.uhn.fhir.jpa.provider.BaseResourceProviderDstu2Test;
import ca.uhn.fhir.jpa.subscription.FhirDstu2Util;
import ca.uhn.fhir.jpa.util.WebsocketSubscriptionClient;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Subscription;
import ca.uhn.fhir.model.dstu2.resource.Subscription.Channel;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionChannelTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionStatusEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;

import static org.hamcrest.Matchers.contains;

// This is currently disabled as the criteria mechanism was a non-standard experiment
@Disabled
public class WebsocketWithCriteriaDstu2Test extends BaseResourceProviderDstu2Test {
    private static final Logger ourLog =
            org.slf4j.LoggerFactory.getLogger(WebsocketWithCriteriaDstu2Test.class);

    @RegisterExtension
    private final WebsocketSubscriptionClient myWebsocketClientExtension =
            new WebsocketSubscriptionClient(() -> myServer, () -> myStorageSettings);

    private String myPatientId;
    private String mySubscriptionId;

    @Override
    @AfterEach
    public void after() throws Exception {
        super.after();
    }

    @Override
    @BeforeEach
    public void before() throws Exception {
        super.before();

        /*
         * Create patient
         */

        Patient patient = FhirDstu2Util.getPatient();
        MethodOutcome methodOutcome = myClient.create().resource(patient).execute();
        myPatientId = methodOutcome.getId().getIdPart();

        /*
         * Create subscription
         */
        Subscription subscription = new Subscription();
        subscription.setReason(
                "Monitor new neonatal function (note, age will be determined by the monitor)");
        subscription.setStatus(SubscriptionStatusEnum.ACTIVE);
        // subscription.setCriteria("Observation?subject=Patient/" + PATIENT_ID);
        subscription.setCriteria("Observation?code=SNOMED-CT|82313006&_format=xml");

        Channel channel = new Channel();
        channel.setType(SubscriptionChannelTypeEnum.WEBSOCKET);
        channel.setPayload("application/json");
        subscription.setChannel(channel);

        methodOutcome = myClient.create().resource(subscription).execute();
        mySubscriptionId = methodOutcome.getId().getIdPart();

        /*
         * Attach websocket
         */

        myWebsocketClientExtension.bind(mySubscriptionId);
    }

    @Test
    public void createObservation() throws Exception {
        Observation observation = new Observation();
        CodeableConceptDt cc = new CodeableConceptDt();
        observation.setCode(cc);
        CodingDt coding = cc.addCoding();
        coding.setCode("82313006");
        coding.setSystem("SNOMED-CT");
        ResourceReferenceDt reference = new ResourceReferenceDt();
        reference.setReference("Patient/" + myPatientId);
        observation.setSubject(reference);
        observation.setStatus(ObservationStatusEnum.FINAL);

        MethodOutcome methodOutcome2 = myClient.create().resource(observation).execute();
        String observationId = methodOutcome2.getId().getIdPart();
        observation.setId(observationId);

        ourLog.info("Observation id generated by server is: " + observationId);

        ourLog.info("WS Messages: {}", myWebsocketClientExtension.getMessages());
        waitForSize(2, myWebsocketClientExtension.getMessages());
        MatcherAssert.assertThat(
                myWebsocketClientExtension.getMessages(),
                contains("bound " + mySubscriptionId, "ping " + mySubscriptionId));
    }

    @Test
    public void createObservationThatDoesNotMatch() throws Exception {
        Observation observation = new Observation();
        CodeableConceptDt cc = new CodeableConceptDt();
        observation.setCode(cc);
        CodingDt coding = cc.addCoding();
        coding.setCode("8231");
        coding.setSystem("SNOMED-CT");
        ResourceReferenceDt reference = new ResourceReferenceDt();
        reference.setReference("Patient/" + myPatientId);
        observation.setSubject(reference);
        observation.setStatus(ObservationStatusEnum.FINAL);

        MethodOutcome methodOutcome2 = myClient.create().resource(observation).execute();
        String observationId = methodOutcome2.getId().getIdPart();
        observation.setId(observationId);

        ourLog.info("Observation id generated by server is: " + observationId);

        waitForSize(2, myWebsocketClientExtension.getMessages());
        ourLog.info("WS Messages: {}", myWebsocketClientExtension.getMessages());
        MatcherAssert.assertThat(
                myWebsocketClientExtension.getMessages(), contains("bound " + mySubscriptionId));
    }
}
