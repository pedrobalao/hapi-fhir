package ca.uhn.fhir.storage.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.util.MetaTagSorterAlphabetical;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;


@Interceptor
public class TagOrderInterceptor {
	private IMetaTagSorter myTagSorter;

	public TagOrderInterceptor(IMetaTagSorter theTagSorter) {
		myTagSorter = theTagSorter;
	}

	private void sortResourceTags(IBaseResource theResource) {
		IBaseMetaType meta = theResource.getMeta();
		myTagSorter.sort(meta);
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
