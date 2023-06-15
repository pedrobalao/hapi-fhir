package ca.uhn.fhir.rest.param;

import java.util.EnumSet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenParamModifierTest {

    @ParameterizedTest
    @EnumSource()
    void negativeModifiers(TokenParamModifier theTokenParamModifier) {
        EnumSet<TokenParamModifier> negativeSet =
                EnumSet.of(TokenParamModifier.NOT, TokenParamModifier.NOT_IN);

        assertEquals(
                negativeSet.contains(theTokenParamModifier), theTokenParamModifier.isNegative());
    }
}
