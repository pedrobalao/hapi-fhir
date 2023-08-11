package ca.uhn.fhir.storage;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.util.CodingAlphabeticalComparator;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.util.PrimitiveTypeOfStringAlphabeticalComparator;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.Comparator;


@Interceptor
public class TagOrderInterceptor {

	private Comparator<IBaseCoding> codingAlphabeticalComparator = new CodingAlphabeticalComparator();
	private Comparator<IPrimitiveType<String>> profileAlphabeticalComparator = new PrimitiveTypeOfStringAlphabeticalComparator();
	private void sortResourceTags(IBaseResource theResource) {
		IBaseMetaType meta = theResource.getMeta();
	   meta.getTag().sort(codingAlphabeticalComparator);
		meta.getSecurity().sort(codingAlphabeticalComparator);
		meta.getProfile().sort(profileAlphabeticalComparator);
	}

	@Hook(value = Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	public void resourcePreCreate(RequestDetails theRequest, IBaseResource theResource) {
		sortResourceTags(theResource);
	}
	@Hook(value = Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
	public void resourcePreUpdate(RequestDetails theRequest, IBaseResource theOldResource, IBaseResource theNewResource) {
		sortResourceTags(theNewResource);
	}

	@Hook(value = Pointcut.STORAGE_PRESHOW_RESOURCES)
	public void resourcePreShow(IPreResourceShowDetails thePreShowDetails) {
		for (IBaseResource resource : thePreShowDetails) {
			sortResourceTags(resource);
		}
	}
}
