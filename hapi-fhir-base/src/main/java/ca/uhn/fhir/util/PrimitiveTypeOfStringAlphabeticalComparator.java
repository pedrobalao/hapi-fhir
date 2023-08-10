package ca.uhn.fhir.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.Comparator;

public class PrimitiveTypeOfStringAlphabeticalComparator implements Comparator<IPrimitiveType<String>> {
	@Override
	public int compare(IPrimitiveType<String> o1, IPrimitiveType<String> o2) {
		return StringUtils.compare(o1.getValue(), o2.getValue());
	}
}
