package com.fingaurd.service;

import com.fingaurd.dto.response.ComponentScoresDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskFusionEngine {

    @Value("${fingaurd.risk.escalation-max:30.0}")
    private double escalationMax;

    @Value("${fingaurd.risk.baseline-max:25.0}")
    private double baselineMax;

    @Value("${fingaurd.risk.velocity-max:20.0}")
    private double velocityMax;

    @Value("${fingaurd.risk.influence-max:25.0}")
    private double influenceMax;

    @Value("${fingaurd.risk.escalation-nudge:5.0}")
    private double escalationNudge;

    @Value("${fingaurd.risk.velocity-normalizer:0.7}")
    private double velocityNormalizer;

    public static class FusionResult {
        private final int overallScore;
        private final String riskLevel;
        private final ComponentScoresDto componentScores;

        public FusionResult(int overallScore, String riskLevel, ComponentScoresDto componentScores) {
            this.overallScore = overallScore;
            this.riskLevel = riskLevel;
            this.componentScores = componentScores;
        }

        public int getOverallScore() {
            return overallScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public ComponentScoresDto getComponentScores() {
            return componentScores;
        }
    }

    public FusionResult computeRisk(JourneyFeatures features, double influenceScore, boolean communicationAnalyzed) {
        // 1. Escalation Sub-Score = 30 * clamp01((historicalMaxRatio - 1.0) / 3.0), nudged upward (capped at 30)
        double histMaxRatio = features.getHistoricalMaxRatio();
        double escalationRaw = escalationMax * DeviationAnomalyDetector.clamp01((histMaxRatio - 1.0) / 3.0);

        if (features.isReturnFollowedByEscalation() || features.getMaxConsecutiveStepRatio() >= 2.0) {
            escalationRaw = Math.min(escalationMax, escalationRaw + escalationNudge);
        }
        double escalationScore = Math.min(escalationMax, escalationRaw);

        // 2. Personal Baseline Deviation = 25 * clamp01(|z| / 3.0)
        double absZ = Math.abs(features.getBaselineZScore());
        double baselineScore = Math.min(baselineMax, baselineMax * DeviationAnomalyDetector.clamp01(absZ / 3.0));

        // 3. Commitment Velocity = 20 * clamp01((velocity / firstInvestmentAmount) / 0.7)
        double velocity = features.getCommitmentVelocity();
        BigDecimal firstAmt = features.getFirstAmount();
        double firstAmtVal = (firstAmt != null && firstAmt.compareTo(BigDecimal.ZERO) > 0)
                ? firstAmt.doubleValue()
                : features.getProposedAmount().doubleValue();

        double velocityRatio = (firstAmtVal > 0) ? (velocity / firstAmtVal) : 0.0;
        double velocityScore = Math.min(velocityMax, velocityMax * DeviationAnomalyDetector.clamp01(velocityRatio / velocityNormalizer));

        // 4. Influence Score
        double finalInfluence = 0.0;
        boolean redistributed = false;
        double totalRaw;

        if (communicationAnalyzed) {
            finalInfluence = Math.min(influenceMax, Math.max(0.0, influenceScore));
            totalRaw = escalationScore + baselineScore + velocityScore + finalInfluence;
        } else {
            // When no communication is supplied or consented: redistribute by 100/75
            redistributed = true;
            double sumThree = escalationScore + baselineScore + velocityScore;
            totalRaw = sumThree * (100.0 / 75.0);
        }

        int finalScore = (int) Math.round(Math.min(100.0, Math.max(0.0, totalRaw)));
        String riskBand = determineRiskBand(finalScore);

        ComponentScoresDto scoresDto = new ComponentScoresDto(
                round(escalationScore, 2),
                round(baselineScore, 2),
                round(velocityScore, 2),
                round(finalInfluence, 2),
                redistributed
        );

        return new FusionResult(finalScore, riskBand, scoresDto);
    }

    public static String determineRiskBand(int score) {
        if (score <= 25) return "LOW";
        if (score <= 50) return "MODERATE";
        if (score <= 75) return "HIGH";
        return "CRITICAL";
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
