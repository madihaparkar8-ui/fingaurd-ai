package com.fingaurd.service;

import com.fingaurd.dto.response.InfluenceAnalysisResponse;
import com.fingaurd.dto.response.InfluenceCategoryDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InfluenceNlpService {

    public static final String CATEGORY_URGENCY = "URGENCY";
    public static final String CATEGORY_GUARANTEED_RETURNS = "GUARANTEED_RETURNS";
    public static final String CATEGORY_PRESSURE_TO_INCREASE = "PRESSURE_TO_INCREASE";
    public static final String CATEGORY_EXCLUSIVITY_FOMO = "EXCLUSIVITY_FOMO";
    public static final String CATEGORY_WITHDRAWAL_FEE = "WITHDRAWAL_OR_UNLOCK_FEE";
    public static final String CATEGORY_AUTHORITY_CLAIM = "AUTHORITY_CLAIM";
    public static final String CATEGORY_REPEATED_PAYMENT = "REPEATED_LARGER_PAYMENT_REQUEST";

    private static final Map<String, List<String>> CATEGORY_PATTERNS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();

    static {
        CATEGORY_LABELS.put(CATEGORY_URGENCY, "Artificial Urgency & Time Pressure");
        CATEGORY_PATTERNS.put(CATEGORY_URGENCY, List.of(
                "limited slots", "last chance today", "hurry up", "valid only for",
                "urgent transfer", "closing in next", "act now", "expires in",
                "offer ends", "within 2 hours", "immediate deposit", "deadline today"
        ));

        CATEGORY_LABELS.put(CATEGORY_GUARANTEED_RETURNS, "Unrealistic Guaranteed Returns");
        CATEGORY_PATTERNS.put(CATEGORY_GUARANTEED_RETURNS, List.of(
                "assured returns", "guaranteed returns", "double your money", "risk-free",
                "fixed profit", "100% safe return", "30% daily", "guaranteed profit",
                "promised return", "zero risk", "multiplied payout", "100% assured"
        ));

        CATEGORY_LABELS.put(CATEGORY_PRESSURE_TO_INCREASE, "Coercive Pressure to Escalate Capital");
        CATEGORY_PATTERNS.put(CATEGORY_PRESSURE_TO_INCREASE, List.of(
                "deposit more", "upgrade your account", "invest larger amount", "scale up your capital",
                "add more funds", "higher tier requires", "top up now", "increase your principal",
                "deposit the full amount", "commit higher capital", "larger deposit required"
        ));

        CATEGORY_LABELS.put(CATEGORY_EXCLUSIVITY_FOMO, "Artificial Exclusivity & FOMO Triggers");
        CATEGORY_PATTERNS.put(CATEGORY_EXCLUSIVITY_FOMO, List.of(
                "vip group", "exclusive group", "private club", "only 3 left",
                "secret allocation", "privileged access", "members only", "limited invitation",
                "miss this opportunity", "insider circle", "special invitation only", "rare window"
        ));

        CATEGORY_LABELS.put(CATEGORY_WITHDRAWAL_FEE, "Withdrawal Fee / Unlock Demands");
        CATEGORY_PATTERNS.put(CATEGORY_WITHDRAWAL_FEE, List.of(
                "pay a small fee to release your withdrawal", "pay tax to withdraw", "unlock fee", "release fee",
                "clearance charge", "processing charge to release", "penalty to withdraw", "unfreeze your account",
                "release your withdrawal fast", "withdrawal approval fee", "tax deposit before payout"
        ));

        CATEGORY_LABELS.put(CATEGORY_AUTHORITY_CLAIM, "Fabricated Authority & Endorsement");
        CATEGORY_PATTERNS.put(CATEGORY_AUTHORITY_CLAIM, List.of(
                "sebi registered guru", "certified financial guru", "institutional insider", "government approved scheme",
                "rbi accredited advisor", "official market maker", "licensed expert", "insider tips",
                "approved by ministry", "official trading mentor", "sebi verified signal"
        ));

        CATEGORY_PATTERNS.put(CATEGORY_REPEATED_PAYMENT, List.of(
                "send another payment", "final payment required", "one more deposit", "second installment needed",
                "balance payment required", "transfer immediately to lock", "one last payment", "additional transfer needed",
                "complete the final stage payment", "subsequent tranche required"
        ));
        CATEGORY_LABELS.put(CATEGORY_REPEATED_PAYMENT, "Sequential Capital Escalation Demands");
    }

    public InfluenceAnalysisResponse analyse(String text, Boolean consentGranted) {
        InfluenceAnalysisResponse resp = new InfluenceAnalysisResponse();
        resp.setText(text);

        if (text == null || text.trim().isEmpty() || !Boolean.TRUE.equals(consentGranted)) {
            resp.setIndicatorsDetected(false);
            resp.setHighestConfidence(0.0);
            resp.setScoreContribution(0.0);
            return resp;
        }

        String lowerText = text.toLowerCase();
        List<InfluenceCategoryDto> detected = new ArrayList<>();
        double highestConf = 0.0;

        for (Map.Entry<String, List<String>> entry : CATEGORY_PATTERNS.entrySet()) {
            String cat = entry.getKey();
            List<String> patterns = entry.getValue();
            List<String> matched = new ArrayList<>();

            for (String p : patterns) {
                Pattern regex = Pattern.compile("\\b" + Pattern.quote(p) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = regex.matcher(text);
                if (matcher.find()) {
                    matched.add(matcher.group());
                } else if (lowerText.contains(p)) {
                    matched.add(p);
                }
            }

            if (!matched.isEmpty()) {
                // Confidence between 0.35 and 1.0 based on matched patterns
                double conf = Math.min(1.0, 0.40 + (matched.size() * 0.20));
                conf = Math.round(conf * 100.0) / 100.0;
                if (conf > highestConf) {
                    highestConf = conf;
                }
                detected.add(new InfluenceCategoryDto(cat, CATEGORY_LABELS.get(cat), conf, matched));
            }
        }

        resp.setDetectedCategories(detected);
        resp.setIndicatorsDetected(!detected.isEmpty());
        resp.setHighestConfidence(highestConf);

        // Score contribution: 25 * (highest category confidence blended with number of categories detected)
        double scoreContribution = 0.0;
        if (!detected.isEmpty()) {
            double categoryBlend = Math.min(1.0, detected.size() / 3.0);
            double blendedFactor = (highestConf * 0.6) + (categoryBlend * 0.4);
            scoreContribution = Math.min(25.0, 25.0 * blendedFactor);
        }
        scoreContribution = Math.round(scoreContribution * 100.0) / 100.0;
        resp.setScoreContribution(scoreContribution);

        return resp;
    }
}
