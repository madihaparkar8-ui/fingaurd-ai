package com.fingaurd.service;

import com.fingaurd.model.Investment;
import com.fingaurd.model.InvestmentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SequenceRuleService {

    public boolean isReturnFollowedByEscalation(List<Investment> sortedHistory, BigDecimal proposedAmount) {
        if (sortedHistory == null || sortedHistory.isEmpty()) {
            return false;
        }

        // Check if proposed commitment follows a return
        Investment last = sortedHistory.get(sortedHistory.size() - 1);
        if (proposedAmount != null && proposedAmount.compareTo(last.getAmount()) > 0) {
            if (last.getStatus() == InvestmentStatus.RETURNED ||
                (last.getReturnAmount() != null && last.getReturnAmount().compareTo(BigDecimal.ZERO) > 0)) {
                return true;
            }
        }

        // Check if any event in the history satisfied this sequence rule
        for (int i = 1; i < sortedHistory.size(); i++) {
            Investment current = sortedHistory.get(i);
            Investment prev = sortedHistory.get(i - 1);
            if (current.getAmount().compareTo(prev.getAmount()) > 0) {
                if (prev.getStatus() == InvestmentStatus.RETURNED ||
                    (prev.getReturnAmount() != null && prev.getReturnAmount().compareTo(BigDecimal.ZERO) > 0)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<String> determineTransitionBadges(List<Investment> historyUpToCurrent, Investment current) {
        List<String> badges = new ArrayList<>();
        if (historyUpToCurrent == null || historyUpToCurrent.size() <= 1) {
            return badges;
        }

        int currentIndex = historyUpToCurrent.size() - 1;
        Investment prev = historyUpToCurrent.get(currentIndex - 1);

        // 1. Step ratio >= 2.0x
        if (prev.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            double ratio = current.getAmount().doubleValue() / prev.getAmount().doubleValue();
            if (ratio >= 2.0) {
                badges.add(String.format("%.1f× previous commitment", ratio));
            }
        }

        // 2. New historical maximum
        BigDecimal prevMax = BigDecimal.ZERO;
        for (int i = 0; i < currentIndex; i++) {
            if (historyUpToCurrent.get(i).getAmount().compareTo(prevMax) > 0) {
                prevMax = historyUpToCurrent.get(i).getAmount();
            }
        }
        if (current.getAmount().compareTo(prevMax) > 0) {
            badges.add("new historical maximum");
        }

        // 3. Rapid escalation (short interval <= 7 days and step > 1.2x)
        long daysBetween = ChronoUnit.DAYS.between(prev.getInvestedOn(), current.getInvestedOn());
        if (daysBetween <= 7 && current.getAmount().compareTo(prev.getAmount()) > 0) {
            badges.add("rapid escalation");
        }

        // 4. Same entity exposure
        if (current.getEntityName() != null && current.getEntityName().equalsIgnoreCase(prev.getEntityName())) {
            badges.add("same entity exposure");
        }

        // 5. Positive outcome followed by larger commitment
        if (current.getAmount().compareTo(prev.getAmount()) > 0) {
            if (prev.getStatus() == InvestmentStatus.RETURNED ||
                (prev.getReturnAmount() != null && prev.getReturnAmount().compareTo(BigDecimal.ZERO) > 0)) {
                badges.add("positive outcome followed by larger commitment");
            }
        }

        return badges;
    }
}
