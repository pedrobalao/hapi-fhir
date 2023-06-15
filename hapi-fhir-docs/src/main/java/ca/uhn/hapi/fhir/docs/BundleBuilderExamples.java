/*-
 * #%L
 * HAPI FHIR - Docs
 * %%
 * Copyright (C) 2014 - 2023 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.hapi.fhir.docs;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleBuilder;

@SuppressWarnings("unused")
public class BundleBuilderExamples {

    private FhirContext myFhirContext;
    private IGenericClient myFhirClient;

    public void update() throws FHIRException {
        // START SNIPPET: update
        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);

        // Create a Patient to update
        Patient patient = new Patient();
        patient.setId("http://foo/Patient/123");
        patient.setActive(true);

        // Add the patient as an update (aka PUT) to the Bundle
        builder.addTransactionUpdateEntry(patient);

        // Execute the transaction
        IBaseBundle outcome = myFhirClient.transaction().withBundle(builder.getBundle()).execute();
        // END SNIPPET: update
    }

    public void updateConditional() throws FHIRException {
        // START SNIPPET: updateConditional
        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);

        // Create a Patient to update
        Patient patient = new Patient();
        patient.setActive(true);
        patient.addIdentifier().setSystem("http://foo").setValue("bar");

        // Add the patient as an update (aka PUT) to the Bundle
        builder.addTransactionUpdateEntry(patient).conditional("Patient?identifier=http://foo|bar");

        // Execute the transaction
        IBaseBundle outcome = myFhirClient.transaction().withBundle(builder.getBundle()).execute();
        // END SNIPPET: updateConditional
    }

    public void create() throws FHIRException {
        // START SNIPPET: create
        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);

        // Create a Patient to create
        Patient patient = new Patient();
        patient.setActive(true);

        // Add the patient as a create (aka POST) to the Bundle
        builder.addTransactionCreateEntry(patient);

        // Execute the transaction
        IBaseBundle outcome = myFhirClient.transaction().withBundle(builder.getBundle()).execute();
        // END SNIPPET: create
    }

    public void createConditional() throws FHIRException {
        // START SNIPPET: createConditional
        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);

        // Create a Patient to create
        Patient patient = new Patient();
        patient.setActive(true);
        patient.addIdentifier().setSystem("http://foo").setValue("bar");

        // Add the patient as a create (aka POST) to the Bundle
        builder.addTransactionCreateEntry(patient).conditional("Patient?identifier=http://foo|bar");

        // Execute the transaction
        IBaseBundle outcome = myFhirClient.transaction().withBundle(builder.getBundle()).execute();
        // END SNIPPET: createConditional
    }

    public void patch() throws FHIRException {
        // START SNIPPET: patch

        // Create a FHIR Patch object
        Parameters patch = new Parameters();
        Parameters.ParametersParameterComponent op = patch.addParameter().setName("operation");
        op.addPart().setName("type").setValue(new CodeType("replace"));
        op.addPart().setName("path").setValue(new CodeType("Patient.active"));
        op.addPart().setName("value").setValue(new BooleanType(false));

        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);

        // Create a target object (this is the ID of the resource that will be patched)
        IIdType targetId = new IdType("Patient/123");

        // Add the patch to the bundle
        builder.addTransactionFhirPatchEntry(targetId, patch);

        // Execute the transaction
        IBaseBundle outcome = myFhirClient.transaction().withBundle(builder.getBundle()).execute();
        // END SNIPPET: patch
    }

    public void patchConditional() throws FHIRException {
        // START SNIPPET: patchConditional

        // Create a FHIR Patch object
        Parameters patch = new Parameters();
        Parameters.ParametersParameterComponent op = patch.addParameter().setName("operation");
        op.addPart().setName("type").setValue(new CodeType("replace"));
        op.addPart().setName("path").setValue(new CodeType("Patient.active"));
        op.addPart().setName("value").setValue(new BooleanType(false));

        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);

        // Add the patch to the bundle with a conditional URL
        String conditionalUrl = "Patient?identifier=http://foo|123";
        builder.addTransactionFhirPatchEntry(patch).conditional(conditionalUrl);

        // Execute the transaction
        IBaseBundle outcome = myFhirClient.transaction().withBundle(builder.getBundle()).execute();
        // END SNIPPET: patchConditional
    }

    public void customizeBundle() throws FHIRException {
        // START SNIPPET: customizeBundle
        // Create a TransactionBuilder
        BundleBuilder builder = new BundleBuilder(myFhirContext);
        // Set bundle type to be searchset
        builder.setBundleField("type", "searchset")
                .setBundleField("id", UUID.randomUUID().toString())
                .setMetaField("lastUpdated", builder.newPrimitive("instant", new Date()));

        // Create bundle entry
        IBase entry = builder.addEntry();

        // Create a Patient to create
        Patient patient = new Patient();
        patient.setActive(true);
        patient.addIdentifier().setSystem("http://foo").setValue("bar");
        builder.addToEntry(entry, "resource", patient);

        // Add search results
        IBase search = builder.addSearch(entry);
        builder.setSearchField(search, "mode", "match");
        builder.setSearchField(search, "score", builder.newPrimitive("decimal", BigDecimal.ONE));
        // END SNIPPET: customizeBundle
    }
}
