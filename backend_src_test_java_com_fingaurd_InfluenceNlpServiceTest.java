package com.fingaurd;

import com.fingaurd.dto.response.InfluenceAnalysisResponse;
import com.fingaurd.service.InfluenceNlpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InfluenceNlpServiceTest {

    private InfluenceNlpService nlpService;

    @BeforeEach
    void setUp() {
        nlpService = new InfluenceNlpService();
    }

    @Test
    @DisplayName("Should detect all 7 categories when targeted phrases are present")
    void testAllCategoriesDetection() {
        String fullText = """
            Limited slots, this is your last chance today!
            We offer assured returns and guaranteed profit with 100% safe return.
            You must deposit more to upgrade your account.
            Join our exclusive VIP group with private club access.
            Pay a small fee to release your withdrawal fast.
            Guided by a SEBI registered guru and RBI accredited advisor.
            Please send another payment to lock the position.
            """;

        InfluenceAnalysisResponse resp = nlpService.analyse(fullText, true);

        assertTrue(resp.getIndicatorsDetected());
        assertEquals(7, resp.getDetectedCategories().size());
        assertTrue(resp.getHighestConfidence() >= 0.8);
        assertTrue(resp.getScoreContribution() > 20.0);
    }

    @Test
    @DisplayName("Should respect user consent: if consent is false, return empty indicators")
    void testConsentRequirement() {
        String text = "Exclusive VIP group closing in next 2 hours! Assured returns.";
        InfluenceAnalysisResponse resp = nlpService.analyse(text, false);

        assertFalse(resp.getIndicatorsDetected());
        assertEquals(0, resp.getDetectedCategories().size());
        assertEquals(0.0, resp.getScoreContribution());
    }

    @Test
    @DisplayName("Should return empty indicators for benign text")
    void testBenignText() {
        String text = "Hi, attached is your mutual fund portfolio statement for September. Please review at your convenience.";
        InfluenceAnalysisResponse resp = nlpService.analyse(text, true);

        assertFalse(resp.getIndicatorsDetected());
        assertEquals(0, resp.getDetectedCategories().size());
        assertEquals(0.0, resp.getScoreContribution());
    }
}
