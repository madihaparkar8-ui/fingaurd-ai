package com.fingaurd;

import com.fingaurd.config.SafetyLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SafetyLanguagePolicyTest {

    @Test
    @DisplayName("Safety policy throws exception if prohibited phrase is introduced")
    void testProhibitedPhrasesCheck() {
        for (String phrase : SafetyLanguage.PROHIBITED_PHRASES) {
            assertThrows(IllegalStateException.class, () ->
                    SafetyLanguage.validateNoProhibitedLanguage("Alert: " + phrase + " on your transaction!"));
        }
    }

    @Test
    @DisplayName("Safety policy allows approved terminology and disclaimers")
    void testApprovedTerminology() {
        assertDoesNotThrow(() -> {
            SafetyLanguage.validateNoProhibitedLanguage(SafetyLanguage.DISCLAIMER);
            SafetyLanguage.validateNoProhibitedLanguage(SafetyLanguage.HIGH_RISK_PATTERN);
            SafetyLanguage.validateNoProhibitedLanguage(SafetyLanguage.MANIPULATION_INDICATORS);
            SafetyLanguage.validateNoProhibitedLanguage(SafetyLanguage.UNUSUAL_COMMITMENT);
            SafetyLanguage.validateNoProhibitedLanguage(SafetyLanguage.PAUSE_AND_VERIFY);
        });
    }

    @Test
    @DisplayName("Disclaimer constant matches exact mandatory wording")
    void testDisclaimerContent() {
        assertEquals("Prototype score — not validated. This is not financial advice and not a fraud verdict.",
                SafetyLanguage.DISCLAIMER);
    }
}
