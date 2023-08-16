package ca.uhn.fhir.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseCoding;

import java.util.Comparator;

public class CodingAlphabeticalComparator implements Comparator<IBaseCoding> {
	@Override
	public int compare(IBaseCoding o1, IBaseCoding o2) {
		int systemCompareResult = StringUtils.compare(o1.getSystem(), o2.getSystem());
		if (systemCompareResult != 0) {
			return systemCompareResult;
		}
		return StringUtils.compare(o1.getCode(), o2.getCode());
	}
}
