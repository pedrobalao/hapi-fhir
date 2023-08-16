package ca.uhn.fhir.util;

import ca.uhn.fhir.storage.interceptor.IMetaTagSorter;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.Comparator;
import java.util.List;

public class MetaTagSorterAlphabetical implements IMetaTagSorter {
	private final Comparator<IBaseCoding> myCodingAlphabeticalComparator = new CodingAlphabeticalComparator();
	private final Comparator<IPrimitiveType<String>> myPrimitiveStringAlphabeticalComparator = new PrimitiveTypeOfStringAlphabeticalComparator();

	public void sortCodings(List<? extends IBaseCoding> theCodings) {
		theCodings.sort(myCodingAlphabeticalComparator);
	}

	 public void sortPrimitiveStrings(List<? extends IPrimitiveType<String>> theList) {
		theList.sort(myPrimitiveStringAlphabeticalComparator);
	}
	public void sort(IBaseMetaType theMeta) {
		sortCodings(theMeta.getTag());
		sortCodings(theMeta.getSecurity());
		sortPrimitiveStrings(theMeta.getProfile());
	}
}
