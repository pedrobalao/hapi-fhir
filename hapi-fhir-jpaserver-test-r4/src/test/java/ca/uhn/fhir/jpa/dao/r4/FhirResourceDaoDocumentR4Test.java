package ca.uhn.fhir.jpa.dao.r4;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.test.BaseJpaR4Test;

public class FhirResourceDaoDocumentR4Test extends BaseJpaR4Test {

    private static final org.slf4j.Logger ourLog =
            org.slf4j.LoggerFactory.getLogger(FhirResourceDaoDocumentR4Test.class);

    @Test
    public void testPostDocument() throws Exception {
        String input =
                IOUtils.toString(
                        getClass().getResourceAsStream("/sample-document.xml"),
                        StandardCharsets.UTF_8);
        Bundle inputBundle = myFhirContext.newXmlParser().parseResource(Bundle.class, input);
        DaoMethodOutcome responseBundle = myBundleDao.create(inputBundle, mySrd);
    }
}
