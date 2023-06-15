package ca.uhn.fhir.mdm.rules.matcher;

import org.slf4j.Logger;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.phonetic.PhoneticEncoderEnum;
import ca.uhn.fhir.jpa.nickname.NicknameSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.EmptyFieldMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.ExtensionMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.HapiDateMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.HapiStringMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.IdentifierMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.MdmNameMatchModeEnum;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.NameMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.NicknameMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.NumericMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.PhoneticEncoderMatcher;
import ca.uhn.fhir.mdm.rules.matcher.fieldmatchers.SubstringStringMatcher;
import ca.uhn.fhir.mdm.rules.matcher.models.IMdmFieldMatcher;
import ca.uhn.fhir.mdm.rules.matcher.models.MatchTypeEnum;

public class MdmMatcherFactory implements IMatcherFactory {
    private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

    private final FhirContext myFhirContext;
    private final IMdmSettings myMdmSettings;

    private final NicknameSvc myNicknameSvc;

    public MdmMatcherFactory(
            FhirContext theFhirContext, IMdmSettings theSettings, NicknameSvc theNicknameSvc) {
        myFhirContext = theFhirContext;
        myMdmSettings = theSettings;
        myNicknameSvc = theNicknameSvc;
    }

    @Override
    public IMdmFieldMatcher getFieldMatcherForMatchType(MatchTypeEnum theMdmMatcherEnum) {
        String matchTypeName;
        if (theMdmMatcherEnum != null) {
            switch (theMdmMatcherEnum) {
                case CAVERPHONE1:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.CAVERPHONE1);
                case CAVERPHONE2:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.CAVERPHONE2);
                case COLOGNE:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.COLOGNE);
                case DOUBLE_METAPHONE:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.DOUBLE_METAPHONE);
                case MATCH_RATING_APPROACH:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.MATCH_RATING_APPROACH);
                case METAPHONE:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.METAPHONE);
                case NYSIIS:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.NYSIIS);
                case REFINED_SOUNDEX:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.REFINED_SOUNDEX);
                case SOUNDEX:
                    return new PhoneticEncoderMatcher(PhoneticEncoderEnum.SOUNDEX);
                case NICKNAME:
                    return new NicknameMatcher(myNicknameSvc);
                case STRING:
                    return new HapiStringMatcher();
                case SUBSTRING:
                    return new SubstringStringMatcher();
                case DATE:
                    return new HapiDateMatcher(myFhirContext);
                case NAME_ANY_ORDER:
                    return new NameMatcher(myFhirContext, MdmNameMatchModeEnum.ANY_ORDER);
                case NAME_FIRST_AND_LAST:
                    return new NameMatcher(myFhirContext, MdmNameMatchModeEnum.FIRST_AND_LAST);
                case IDENTIFIER:
                    return new IdentifierMatcher();
                case EXTENSION_ANY_ORDER:
                    return new ExtensionMatcher();
                case NUMERIC:
                    return new NumericMatcher();
                case EMPTY_FIELD:
                    return new EmptyFieldMatcher();
                default:
                    break;
            }
            matchTypeName = theMdmMatcherEnum.name();
        } else {
            matchTypeName = "null";
        }

        // This is odd, but it's a valid code path
        ourLog.warn("Unrecognized field type {}. Returning null", matchTypeName);
        return null;
    }
}
