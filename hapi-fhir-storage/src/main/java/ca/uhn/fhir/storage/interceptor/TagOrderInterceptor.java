package ca.uhn.fhir.storage.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * An interceptor that can be used to sort Meta resource properties that store sets: tags, security labels and profiles.
 * It delegates the actual sorting of the Meta properties to an implementation of {@link IMetaTagSorter} interface,
 * which is called with the Meta object as an argument for pointcuts before a resource being saved/updated and returned to the caller.
 * <p>
 * An implementation of {@link IMetaTagSorter} is {@link  ca.uhn.fhir.util.MetaTagSorterAlphabetical} which sorts
 * meta collections in alphabetical order. It sorts sets containing Coding types (i.e., tags and security label)
 * based on their (system, code) value pair and profiles based on the string value.
 *
 */
@Interceptor
public class TagOrderInterceptor {
	private IMetaTagSorter myTagSorter;

	public TagOrderInterceptor(IMetaTagSorter theTagSorter) {
		myTagSorter = theTagSorter;
	}

	@Hook(value = Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	public void resourcePreCreate(RequestDetails theRequest, IBaseResource theResource) {
		sortResourceTags(theResource);
	}

	@Hook(value = Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
	public void resourcePreUpdate(
			RequestDetails theRequest, IBaseResource theOldResource, IBaseResource theNewResource) {
		sortResourceTags(theNewResource);
	}

	@Hook(value = Pointcut.STORAGE_PRESHOW_RESOURCES)
	public void resourcePreShow(IPreResourceShowDetails thePreShowDetails) {
		for (IBaseResource resource : thePreShowDetails) {
			sortResourceTags(resource);
		}
	}

	private void sortResourceTags(IBaseResource theResource) {
		IBaseMetaType meta = theResource.getMeta();
		myTagSorter.sort(meta);
	}
}
