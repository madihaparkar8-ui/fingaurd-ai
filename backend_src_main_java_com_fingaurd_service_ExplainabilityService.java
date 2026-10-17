package com.fingaurd.service;

import com.fingaurd.dto.response.AnomalyEnsembleDto;
import com.fingaurd.dto.response.BehaviouralBiasDto;
import com.fingaurd.dto.response.InfluenceAnalysisResponse;
import com.fingaurd.dto.response.RiskSignalDto;
import com.fingaurd.dto.response.RiskSignalGroupResponse;
import com.fingaurd.model.Investment;
import com.fingaurd.model.RiskSignal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ExplainabilityService {

    private final NumberFormat inCurrency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public List<RiskSignal> generateSignals(JourneyFeatures features,
                                            AnomalyEnsembleDto anomaly,
                                            InfluenceAnalysisResponse influence,
                                            List<Investment> history) {
        List<RiskSignal> signals = new ArrayList<>();
        Long lastEventId = (history != null && !history.isEmpty())
                ? history.get(history.size() - 1).getInvestmentId()
                : null;

        // 1. Historical Max Escalation Signal
        if (features.getHistoricalMaxRatio() > 1.2) {
            String expl = String.format("Proposed commitment of %s is %.1f× higher than your previous highest commitment of %s.",
                    formatInr(features.getProposedAmount()),
                    features.getHistoricalMaxRatio(),
                    formatInr(features.getHistoricalMax()));
            signals.add(new RiskSignal("HISTORICAL_MAX_ESCALATION", String.format("%.1f×", features.getHistoricalMaxRatio()), expl, lastEventId));
        }

        // 2. Personal Baseline Z-Score Signal
        if (Math.abs(features.getBaselineZScore()) >= 2.0) {
            String expl = String.format("This commitment is unusual relative to your history (z-score: %.1f standard deviations above your historical baseline average of %s).",
                    features.getBaselineZScore(),
                    formatInr(features.getBaselineMean()));
            signals.add(new RiskSignal("BASELINE_DEVIATION", String.format("+%.1fσ", features.getBaselineZScore()), expl, lastEventId));
        }

        // 3. Commitment Velocity Signal
        if (features.getCommitmentVelocity() > 1000) {
            String expl = String.format("Commitment velocity has reached %s/day, representing an intense capital accumulation rate over the journey duration.",
                    formatInr(BigDecimal.valueOf(features.getCommitmentVelocity())));
            signals.add(new RiskSignal("COMMITMENT_VELOCITY", String.format("%s/day", formatInr(BigDecimal.valueOf(features.getCommitmentVelocity()))), expl, null));
        }

        // 4. Return Followed By Escalation (Sequence Rule)
        if (features.isReturnFollowedByEscalation()) {
            String expl = "High-risk behavioural pattern detected: A substantial commitment escalation directly follows an initial small return or payout, characteristic of grooming journeys.";
            signals.add(new RiskSignal("SEQUENCE_RETURN_ESCALATION", "Detected", expl, lastEventId));
        }

        // 5. Sudden Jump
        if (features.getMaxConsecutiveStepRatio() >= 2.0) {
            String expl = String.format("Significant jump detected in consecutive transaction sizing (peak consecutive ratio of %.1f× between adjacent commitments).",
                    features.getMaxConsecutiveStepRatio());
            signals.add(new RiskSignal("SUDDEN_JUMP", String.format("%.1f×", features.getMaxConsecutiveStepRatio()), expl, lastEventId));
        }

        // 6. Entity Concentration
        if (features.getEntityConcentration() >= 0.70) {
            String expl = String.format("Heavy capital concentration: %.1f%% of all cumulative commitments are directed towards a single entity.",
                    features.getEntityConcentration() * 100.0);
            signals.add(new RiskSignal("ENTITY_CONCENTRATION", String.format("%.1f%%", features.getEntityConcentration() * 100.0), expl, null));
        }

        // 7. Influence / Communication Signals
        if (influence != null && Boolean.TRUE.equals(influence.getIndicatorsDetected())) {
            influence.getDetectedCategories().forEach(cat -> {
                String expl = String.format("Manipulation indicators detected in communication under category '%s' (confidence: %.0f%%). Matched phrasing: %s.",
                        cat.getLabel(),
                        cat.getConfidence() * 100.0,
                        String.join(", ", cat.getMatchedEvidence()));
                signals.add(new RiskSignal("INFLUENCE_" + cat.getCategory(), String.format("%.0f%%", cat.getConfidence() * 100.0), expl, null));
            });
        }

        return signals;
    }

    public RiskSignalGroupResponse groupSignals(List<RiskSignal> signals) {
        List<RiskSignalDto> whatChanged = new ArrayList<>();
        List<RiskSignalDto> whyItMatters = new ArrayList<>();
        List<RiskSignalDto> whatCanIVerify = new ArrayList<>();

        if (signals != null) {
            for (RiskSignal s : signals) {
                String type = s.getType();
                if (type.contains("ESCALATION") || type.contains("JUMP") || type.contains("VELOCITY")) {
                    whatChanged.add(RiskSignalDto.fromEntity(s, "what_changed"));
                } else if (type.contains("SEQUENCE") || type.contains("BASELINE") || type.contains("INFLUENCE") || type.contains("CONCENTRATION")) {
                    whyItMatters.add(RiskSignalDto.fromEntity(s, "why_it_matters"));
                } else {
                    whatChanged.add(RiskSignalDto.fromEntity(s, "what_changed"));
                }
            }
        }

        // Verification guidance items (What can I verify?)
        whatCanIVerify.add(new RiskSignalDto(null, "VERIFY_SEBI_REGISTRATION", "Official SEBI Portal",
                "Verify whether the intermediary or entity is registered on the official SEBI portal (sebi.gov.in) under Recognised Intermediaries.", null, "verify"));
        whatCanIVerify.add(new RiskSignalDto(null, "VERIFY_MCA_MASTER_DATA", "MCA21 Portal",
                "Verify the corporate status, registered office, and directors on the Ministry of Corporate Affairs (MCA) master data.", null, "verify"));
        whatCanIVerify.add(new RiskSignalDto(null, "VERIFY_RBI_ALERT_LIST", "RBI Sakshi Alert List",
                "Check the RBI Alert List of unauthorized entities and unauthorized forex/crypto trading applications (rbi.org.in).", null, "verify"));
        whatCanIVerify.add(new RiskSignalDto(null, "VERIFY_NO_WITHDRAWAL_FEES", "Security Protocol",
                "Legitimate regulatory authorities never demand advance 'fees', 'taxes', or 'security deposits' to release your investment withdrawals.", null, "verify"));
        whatCanIVerify.add(new RiskSignalDto(null, "REPORT_CYBERCRIME_1930", "Helpline 1930",
                "If pressure or unauthorized demands persist, immediately dial 1930 or lodge an official complaint on cybercrime.gov.in.", null, "verify"));

        return new RiskSignalGroupResponse(whatChanged, whyItMatters, whatCanIVerify);
    }

    public List<BehaviouralBiasDto> detectBehaviouralBiases(JourneyFeatures features,
                                                            InfluenceAnalysisResponse influence,
                                                            List<Investment> history) {
        List<BehaviouralBiasDto> biases = new ArrayList<>();

        // 1. Sunk Cost Fallacy & Escalation of Commitment
        if (features.getHistoricalMaxRatio() >= 2.0 || features.getMaxConsecutiveStepRatio() >= 2.0) {
            biases.add(new BehaviouralBiasDto(
                    "SUNK_COST_FALLACY",
                    "Sunk Cost Fallacy & Escalation of Commitment",
                    "HIGH",
                    String.format("Proposed commitment is %.1f× historical maximum, showing progressive capital entrapment.", features.getHistoricalMaxRatio()),
                    "Investors commit larger sums to recover or justify previous outlays, fearing that stopping forfeits their cumulative investment.",
                    "Evaluate the proposed capital allocation in complete isolation. If you had zero rupees currently invested here, would you initiate fresh contact with this firm?"
            ));
        }

        // 2. Overconfidence & Honeypot Recency Bias
        if (features.isReturnFollowedByEscalation()) {
            biases.add(new BehaviouralBiasDto(
                    "RECENCY_OVERCONFIDENCE",
                    "Honeypot Recency & Illusory Control",
                    "HIGH",
                    "An outsized commitment escalation immediately follows an apparent return payout.",
                    "An artificial small early payout induces an exaggerated feeling of trust and safety, disabling rational due diligence before the main trap.",
                    "Recognize that early returns in unregulated advisory schemes are often fictitious or paid from subsequent deposits. Verify independent custody of assets."
            ));
        }

        // 3. Loss Aversion & Panic Payout Trap
        if (influence != null && Boolean.TRUE.equals(influence.getIndicatorsDetected())) {
            boolean hasFeeCategory = influence.getDetectedCategories().stream()
                    .anyMatch(c -> c.getCategory().contains("WITHDRAWAL"));
            if (hasFeeCategory) {
                biases.add(new BehaviouralBiasDto(
                        "LOSS_AVERSION_PANIC",
                        "Loss Aversion & Panic Clearance Trap",
                        "HIGH",
                        "Communication demands a payment/tax deposit to release user withdrawals.",
                        "The terror of losing accumulated balances triggers panic compliance with demands for additional upfront clearance fees.",
                        "Strict Rule: Never transfer additional capital to withdraw your own capital. Legitimate institutions debit fees internally from account balances."
                ));
            }

            // 4. Authority Deference Bias
            boolean hasAuthority = influence.getDetectedCategories().stream()
                    .anyMatch(c -> c.getCategory().contains("AUTHORITY"));
            if (hasAuthority) {
                biases.add(new BehaviouralBiasDto(
                        "AUTHORITY_DEFERENCE",
                        "Authority Bias & Fictitious Regulatory Endorsement",
                        "MODERATE",
                        "Communication cites 'SEBI registered guru', institutional insiders, or RBI accredited advisors.",
                        "Uncritical psychological deference to purported regulatory authority or prestige badges without checking official databases.",
                        "Check the official SEBI directory at sebi.gov.in. Regulated intermediaries are prohibited from promising fixed or guaranteed returns."
                ));
            }

            // 5. Artificial Scarcity & FOMO
            boolean hasUrgency = influence.getDetectedCategories().stream()
                    .anyMatch(c -> c.getCategory().contains("URGENCY") || c.getCategory().contains("EXCLUSIVITY"));
            if (hasUrgency) {
                biases.add(new BehaviouralBiasDto(
                        "SCARCITY_FOMO",
                        "Artificial Scarcity & Fast-Brain FOMO",
                        "HIGH",
                        "Urgency triggers detected ('limited slots', 'closing in 2 hours', 'last chance today').",
                        "Time pressure deliberately triggers fast-brain System 1 emotional impulse, disabling slow System 2 rational analysis.",
                        "Enforce a mandatory 24-Hour Cooling-Off Period. Legitimate financial instruments and registered funds never vanish in two hours."
                ));
            }
        }

        return biases;
    }

    private String formatInr(BigDecimal amount) {
        if (amount == null) return "₹0";
        return inCurrency.format(amount);
    }
}
