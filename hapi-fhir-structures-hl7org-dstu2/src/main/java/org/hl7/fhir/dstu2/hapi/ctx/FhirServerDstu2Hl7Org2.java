package org.hl7.fhir.dstu2.hapi.ctx;

import org.hl7.fhir.dstu2.hapi.rest.server.ServerConformanceProvider;

import ca.uhn.fhir.rest.api.server.IFhirVersionServer;
import ca.uhn.fhir.rest.server.RestfulServer;

public class FhirServerDstu2Hl7Org2 implements IFhirVersionServer {

    @Override
    public ServerConformanceProvider createServerConformanceProvider(RestfulServer theServer) {
        return new ServerConformanceProvider(theServer);
    }
}
