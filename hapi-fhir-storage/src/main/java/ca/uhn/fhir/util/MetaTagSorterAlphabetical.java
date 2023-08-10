package ca.uhn.fhir.util;

import ca.uhn.fhir.storage.interceptor.IMetaTagSorter;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.Comparator;
import java.util.List;

/**
 *  Contains methods to sort resource meta fields that are sets (i.e., tags, security labels and profiles) in alphabetical order.
 *  It sorts the Coding type sets (tags and security labels) based on the (system, code) pair.
 *  The system field has higher priority on sorting than the code field so the Coding set will be sorted first by system
 *  and then by code for each system.
 */
public class MetaTagSorterAlphabetical implements IMetaTagSorter {
	private final Comparator<IBaseCoding> myCodingAlphabeticalComparator = new CodingAlphabeticalComparator();
	private final Comparator<IPrimitiveType<String>> myPrimitiveStringAlphabeticalComparator =
			new PrimitiveTypeOfStringAlphabeticalComparator();

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
