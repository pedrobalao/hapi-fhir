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

	public static List<Coding> generateAllCodingPairs(List<String> systems, List<String> codes) {
		List<Coding> result = new ArrayList<>();
		for (String system: systems) {
			for (String code: codes) {
				result.add(createCoding(system, code));
			}
		}
		return result;
	}



	public static void assertCodingsEqual(List<Coding> expectedCodings, List<Coding> actualCodings) {
		assertEquals(expectedCodings.size(), actualCodings.size());

		for (int index = 0; index < expectedCodings.size(); index++) {
			final Coding expectedCoding = expectedCodings.get(index);
			final Coding actualCoding = actualCodings.get(index);

			assertAll(
				() -> assertEquals(expectedCoding.getSystem(), actualCoding.getSystem()),
				() -> assertEquals(expectedCoding.getCode(), actualCoding.getCode()),
				() -> assertEquals(expectedCoding.getDisplay(), actualCoding.getDisplay()),
				() -> assertEquals(expectedCoding.getVersion(), actualCoding.getVersion()),
				() -> assertEquals(expectedCoding.getUserSelected(), actualCoding.getUserSelected())
			);
		}
	}

	public static List<String> toStringList(List<CanonicalType> canonicalTypes) {
		return canonicalTypes.stream().map(c -> c.getValue()).collect(Collectors.toList());
	}

	public static Coding createCoding(String system, String code) {
		return createCoding(null, false, code, null, system);
	}

	public static Coding createCoding(String version, boolean userSelected, String code, String display, String system) {
		final Coding coding = new Coding();
		coding.setVersion(version);
		coding.setUserSelected(userSelected);
		coding.setCode(code);
		coding.setDisplay(display);
		coding.setSystem(system);
		return coding;
	}

	public static Meta createMeta(List<Coding> tags, List<Coding> securityLabels, List<String> profiles) {
		Meta meta = new Meta();
		meta.setTag(new ArrayList<>(tags));
		meta.setSecurity(new ArrayList<>(securityLabels));
		meta.setProfile(profiles.stream().map(p -> new CanonicalType(p)).collect(Collectors.toList()));
		return meta;
	}




}
