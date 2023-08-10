package ca.uhn.fhir.storage.interceptor;

import org.hl7.fhir.instance.model.api.IBaseMetaType;

public interface IMetaTagSorter {
	void sort(IBaseMetaType theMeta);
}
