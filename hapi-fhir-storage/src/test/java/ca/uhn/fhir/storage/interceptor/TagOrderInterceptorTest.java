package ca.uhn.fhir.storage.interceptor;

import ca.uhn.fhir.rest.api.server.SimplePreResourceShowDetails;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class TagOrderInterceptorTest {

	private TagOrderInterceptor myTagOrderInterceptor;
	@Mock
	IMetaTagSorter myMockTagSorter;

	@BeforeEach
	public void beforeEach() {
		myTagOrderInterceptor = new TagOrderInterceptor(myMockTagSorter);
	}

	@Test
	public void testCallsSort_OnPreStorageResourceCreated() {
		Patient resource = new Patient();
		Meta meta = new Meta();
		resource.setMeta(meta);

		myTagOrderInterceptor.resourcePreCreate(null, resource);

		Mockito.verify(myMockTagSorter, Mockito.times(1)).sort(meta);
	}

	@Test
	public void testCallsSortOnNewResource_OnPreStorageResourceUpdated() {
		Patient newResource = new Patient();
		Meta meta = new Meta();
		newResource.setMeta(meta);

		myTagOrderInterceptor.resourcePreUpdate(null, null, newResource);

		Mockito.verify(myMockTagSorter, Mockito.times(1)).sort(meta);
	}


	@Test
	public void testCallsSortOnEachResource_OnStoragePreShowResource() {
		Patient resource1 = new Patient();
		Meta meta1 = new Meta();
		resource1.setMeta(meta1);

		Patient resource2 = new Patient();
		Meta meta2 = new Meta();
		resource2.setMeta(meta2);
	   myTagOrderInterceptor.resourcePreShow(new SimplePreResourceShowDetails(List.of(resource1, resource2)));

		Mockito.verify(myMockTagSorter, Mockito.times(1)).sort(meta1);
		Mockito.verify(myMockTagSorter, Mockito.times(1)).sort(meta2);
	}


}
