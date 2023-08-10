package ca.uhn.fhir.test.utilities;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagTestUtil {

	/**
	 * generates a list that contains of all possible Coding pairs from the given system and code values in the iteration order of the lists.
	 * For example: generateAllCodings(["s1", "s2"], ["c1", "c2"]) creates a coding list that contains 4 codings in this order:
	 * [("s1", "c1"), ("s1", "c2"), ("s2", "c1"), ("s2", "c2")].
	 * @param theSystems
	 * @param theCodes
	 * @return
	 */
	public static List<Coding> generateAllCodingPairs(List<String> theSystems, List<String> theCodes) {
		List<Coding> result = new ArrayList<>();
		for (String system: theSystems) {
			for (String code: theCodes) {
				result.add(createCoding(system, code));
			}
		}
		return result;
	}

	/**
	 * asserts that the two coding list contain equal codings (in the same order)
	 * @param theExpectedCodings
	 * @param theActualCodings
	 */
	public static void assertCodingsEqualAndInOrder(List<Coding> theExpectedCodings, List<Coding> theActualCodings) {
		assertEquals(theExpectedCodings.size(), theActualCodings.size());

		for (int index = 0; index < theExpectedCodings.size(); index++) {
			final Coding expectedCoding = theExpectedCodings.get(index);
			final Coding actualCoding = theActualCodings.get(index);

			assertAll(
				() -> assertEquals(expectedCoding.getSystem(), actualCoding.getSystem()),
				() -> assertEquals(expectedCoding.getCode(), actualCoding.getCode()),
				() -> assertEquals(expectedCoding.getDisplay(), actualCoding.getDisplay()),
				() -> assertEquals(expectedCoding.getVersion(), actualCoding.getVersion()),
				() -> assertEquals(expectedCoding.getUserSelected(), actualCoding.getUserSelected())
			);
		}
	}

	public static Coding createCoding(String theSystem, String theCode) {
		return createCoding(null, false, theCode, null, theSystem);
	}

	public static Coding createCoding(String theVersion, boolean theUserSelected, String theCode, String theDisplay, String theSystem) {
		final Coding coding = new Coding();
		coding.setVersion(theVersion);
		coding.setUserSelected(theUserSelected);
		coding.setCode(theCode);
		coding.setDisplay(theDisplay);
		coding.setSystem(theSystem);
		return coding;
	}

	public static Meta createMeta(List<Coding> theTags, List<Coding> theSecurityLabels, List<String> theProfiles) {
		Meta meta = new Meta();
		meta.setTag(new ArrayList<>(theTags));
		meta.setSecurity(new ArrayList<>(theSecurityLabels));
		meta.setProfile(toCanonicalTypeList(theProfiles));
		return meta;
	}

	public static List<CanonicalType> toCanonicalTypeList(List<String> theStrings) {
		return theStrings.stream().map(s -> new CanonicalType(s)).collect(Collectors.toList());
	}

	public static List<String> toStringList(List<CanonicalType> theCanonicalTypes) {
		return theCanonicalTypes.stream().map(c -> c.getValue()).collect(Collectors.toList());
	}
}
