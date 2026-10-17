package com.fingaurd.service;

import com.fingaurd.exception.BadRequestException;
import com.fingaurd.model.Investment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class JourneyFeatureService {

    private final SequenceRuleService sequenceRuleService;

    public JourneyFeatureService(SequenceRuleService sequenceRuleService) {
        this.sequenceRuleService = sequenceRuleService;
    }

    public List<Investment> normaliseAndSort(List<Investment> investments) {
        if (investments == null || investments.isEmpty()) {
            return new ArrayList<>();
        }

        List<Investment> sorted = new ArrayList<>(investments);
        for (Investment inv : sorted) {
            if (inv.getAmount() == null || inv.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Investment amount must be positive.");
            }
            if (inv.getInvestedOn() == null) {
                throw new BadRequestException("Investment date cannot be null.");
            }
            if (inv.getEntityName() == null || inv.getEntityName().trim().isEmpty()) {
                throw new BadRequestException("Entity name cannot be empty.");
            }
        }

        sorted.sort(Comparator.comparing(Investment::getInvestedOn).thenComparing(Investment::getInvestmentId, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    public JourneyFeatures extractFeatures(List<Investment> rawHistory, BigDecimal proposedAmount, LocalDate proposedDate, String proposedEntity) {
        List<Investment> history = normaliseAndSort(rawHistory);
        JourneyFeatures features = new JourneyFeatures();

        features.setProposedAmount(proposedAmount);

        if (history.isEmpty()) {
            features.setFirstAmount(proposedAmount);
            features.setLastAmount(proposedAmount);
            features.setHistoricalMax(proposedAmount);
            features.setBaselineMean(proposedAmount);
            features.setPopulationStdDev(0.0);
            features.setAmountEscalation(BigDecimal.ZERO);
            features.setEscalationRatio(1.0);
            features.setHistoricalMaxRatio(1.0);
            features.setBaselineZScore(0.0);
            features.setCommitmentVelocity(0.0);
            features.setInvestmentFrequency30d(1);
            features.setEntityConcentration(1.0);
            features.setMaxConsecutiveStepRatio(1.0);
            features.setReturnFollowedByEscalation(false);
            features.setEntityTotals(Map.of(proposedEntity, proposedAmount));
            return features;
        }

        int n = history.size();
        BigDecimal firstAmount = history.get(0).getAmount();
        BigDecimal lastAmount = history.get(n - 1).getAmount();
        LocalDate firstDate = history.get(0).getInvestedOn();

        features.setFirstAmount(firstAmount);
        features.setLastAmount(lastAmount);

        // 1. Mean & Max & Entity Totals
        BigDecimal totalSum = BigDecimal.ZERO;
        BigDecimal maxAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> entityTotals = new HashMap<>();

        for (Investment inv : history) {
            BigDecimal amt = inv.getAmount();
            totalSum = totalSum.add(amt);
            if (amt.compareTo(maxAmount) > 0) {
                maxAmount = amt;
            }
            entityTotals.merge(inv.getEntityName(), amt, BigDecimal::add);
        }

        BigDecimal mean = totalSum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        features.setBaselineMean(mean);
        features.setHistoricalMax(maxAmount);
        features.setEntityTotals(entityTotals);

        // 2. Population Standard Deviation: sqrt( 1/N * sum((x_i - mean)^2) )
        double meanVal = mean.doubleValue();
        double sumSqDiff = 0.0;
        for (Investment inv : history) {
            double diff = inv.getAmount().doubleValue() - meanVal;
            sumSqDiff += diff * diff;
        }
        double populationVariance = sumSqDiff / n;
        double populationStdDev = Math.sqrt(populationVariance);
        features.setPopulationStdDev(populationStdDev);

        // 3. Amount Escalation & Escalation Ratio
        BigDecimal amountEscalation = proposedAmount.subtract(lastAmount);
        features.setAmountEscalation(amountEscalation);

        double lastVal = lastAmount.doubleValue();
        double propVal = proposedAmount.doubleValue();
        features.setEscalationRatio(lastVal > 0 ? propVal / lastVal : 1.0);

        // 4. Historical Max Ratio
        double maxVal = maxAmount.doubleValue();
        features.setHistoricalMaxRatio(maxVal > 0 ? propVal / maxVal : 1.0);

        // 5. Baseline Z-Score: (P - mu) / sigma
        if (populationStdDev > 0.0001) {
            double z = (propVal - meanVal) / populationStdDev;
            features.setBaselineZScore(z);
        } else {
            features.setBaselineZScore(0.0);
        }

        // 6. Commitment Velocity: (P - first) / days between first and proposed (min 1 day), in ₹/day
        long days = ChronoUnit.DAYS.between(firstDate, proposedDate);
        if (days < 1) {
            days = 1;
        }
        double velocity = (propVal - firstAmount.doubleValue()) / (double) days;
        features.setCommitmentVelocity(velocity);

        // 7. Investment Frequency in last 30 days
        LocalDate thirtyDaysBefore = proposedDate.minusDays(30);
        int freqCount = 0;
        for (Investment inv : history) {
            if (!inv.getInvestedOn().isBefore(thirtyDaysBefore) && !inv.getInvestedOn().isAfter(proposedDate)) {
                freqCount++;
            }
        }
        features.setInvestmentFrequency30d(freqCount);

        // 8. Entity Concentration: sum to proposed entity / total sum (including proposed)
        BigDecimal proposedEntityHist = entityTotals.getOrDefault(proposedEntity, BigDecimal.ZERO);
        BigDecimal proposedEntityTotal = proposedEntityHist.add(proposedAmount);
        BigDecimal grandTotal = totalSum.add(proposedAmount);
        double concentration = grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? proposedEntityTotal.divide(grandTotal, 4, RoundingMode.HALF_UP).doubleValue()
                : 1.0;
        features.setEntityConcentration(concentration);

        // 9. Sudden Jump / Max Consecutive Step Ratio (across journey, including proposed step)
        double maxStepRatio = 1.0;
        for (int i = 1; i < history.size(); i++) {
            double prevAmt = history.get(i - 1).getAmount().doubleValue();
            double curAmt = history.get(i).getAmount().doubleValue();
            if (prevAmt > 0) {
                double r = curAmt / prevAmt;
                if (r > maxStepRatio) maxStepRatio = r;
            }
        }
        if (lastVal > 0) {
            double propStepRatio = propVal / lastVal;
            if (propStepRatio > maxStepRatio) {
                maxStepRatio = propStepRatio;
            }
        }
        features.setMaxConsecutiveStepRatio(maxStepRatio);

        // 10. Sequence Rule: return followed by escalation
        boolean returnEscalation = sequenceRuleService.isReturnFollowedByEscalation(history, proposedAmount);
        features.setReturnFollowedByEscalation(returnEscalation);

        return features;
    }
}
