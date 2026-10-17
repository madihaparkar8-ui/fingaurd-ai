package com.fingaurd.service;

import com.fingaurd.dto.response.JourneyEventResponse;
import com.fingaurd.dto.response.JourneyResponse;
import com.fingaurd.model.Investment;
import com.fingaurd.model.Journey;
import com.fingaurd.model.User;
import com.fingaurd.repository.InvestmentRepository;
import com.fingaurd.repository.JourneyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class JourneyService {

    private final JourneyRepository journeyRepository;
    private final InvestmentRepository investmentRepository;
    private final SequenceRuleService sequenceRuleService;
    private final JourneyFeatureService journeyFeatureService;

    public JourneyService(JourneyRepository journeyRepository,
                          InvestmentRepository investmentRepository,
                          SequenceRuleService sequenceRuleService,
                          JourneyFeatureService journeyFeatureService) {
        this.journeyRepository = journeyRepository;
        this.investmentRepository = investmentRepository;
        this.sequenceRuleService = sequenceRuleService;
        this.journeyFeatureService = journeyFeatureService;
    }

    @Transactional
    public Journey getOrCreateUserJourney(User user) {
        List<Investment> investments = investmentRepository.findByUser_UserIdOrderByInvestedOnAsc(user.getUserId());
        investments = journeyFeatureService.normaliseAndSort(investments);

        Journey journey = journeyRepository.findTopByUser_UserIdOrderByJourneyIdDesc(user.getUserId())
                .orElseGet(() -> new Journey(user, null, null, 0, BigDecimal.ZERO));

        if (investments.isEmpty()) {
            journey.setEventCount(0);
            journey.setTotalExposure(BigDecimal.ZERO);
            journey.setStartDate(null);
            journey.setEndDate(null);
        } else {
            journey.setEventCount(investments.size());
            journey.setStartDate(investments.get(0).getInvestedOn());
            journey.setEndDate(investments.get(investments.size() - 1).getInvestedOn());

            BigDecimal total = BigDecimal.ZERO;
            for (Investment inv : investments) {
                total = total.add(inv.getAmount());
            }
            journey.setTotalExposure(total);
        }

        return journeyRepository.save(journey);
    }

    @Transactional(readOnly = true)
    public JourneyResponse reconstructUserJourney(Long userId) {
        List<Investment> investments = investmentRepository.findByUser_UserIdOrderByInvestedOnAsc(userId);
        investments = journeyFeatureService.normaliseAndSort(investments);

        Journey journey = journeyRepository.findTopByUser_UserIdOrderByJourneyIdDesc(userId)
                .orElse(null);

        JourneyResponse resp = new JourneyResponse();
        resp.setUserId(userId);
        if (journey != null) {
            resp.setJourneyId(journey.getJourneyId());
        }

        if (investments.isEmpty()) {
            resp.setEventCount(0);
            resp.setTotalExposure(BigDecimal.ZERO);
            resp.setMaxSingleCommitment(BigDecimal.ZERO);
            resp.setMean(BigDecimal.ZERO);
            resp.setStandardDeviation(0.0);
            resp.setDurationDays(0L);
            return resp;
        }

        resp.setEventCount(investments.size());
        resp.setStartDate(investments.get(0).getInvestedOn());
        resp.setEndDate(investments.get(investments.size() - 1).getInvestedOn());

        long duration = ChronoUnit.DAYS.between(resp.getStartDate(), resp.getEndDate());
        resp.setDurationDays(Math.max(1L, duration));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal maxSingle = BigDecimal.ZERO;
        for (Investment inv : investments) {
            total = total.add(inv.getAmount());
            if (inv.getAmount().compareTo(maxSingle) > 0) {
                maxSingle = inv.getAmount();
            }
        }
        resp.setTotalExposure(total);
        resp.setMaxSingleCommitment(maxSingle);

        BigDecimal mean = total.divide(BigDecimal.valueOf(investments.size()), 2, RoundingMode.HALF_UP);
        resp.setMean(mean);

        double meanDouble = mean.doubleValue();
        double sumSq = 0.0;
        for (Investment inv : investments) {
            double diff = inv.getAmount().doubleValue() - meanDouble;
            sumSq += diff * diff;
        }
        double stdDev = Math.sqrt(sumSq / investments.size());
        resp.setStandardDeviation(Math.round(stdDev * 100.0) / 100.0);

        // Build annotated event list
        List<JourneyEventResponse> eventResponses = new ArrayList<>();
        List<Investment> runningHistory = new ArrayList<>();

        for (int i = 0; i < investments.size(); i++) {
            Investment inv = investments.get(i);
            runningHistory.add(inv);

            JourneyEventResponse evResp = new JourneyEventResponse();
            evResp.setInvestmentId(inv.getInvestmentId());
            evResp.setEntityName(inv.getEntityName());
            evResp.setAmount(inv.getAmount());
            evResp.setInvestedOn(inv.getInvestedOn());
            evResp.setType(inv.getType());
            evResp.setReturnAmount(inv.getReturnAmount());
            evResp.setStatus(inv.getStatus());

            if (i > 0) {
                double prevAmt = investments.get(i - 1).getAmount().doubleValue();
                if (prevAmt > 0) {
                    double ratio = inv.getAmount().doubleValue() / prevAmt;
                    evResp.setStepRatio(Math.round(ratio * 10.0) / 10.0);
                }
            } else {
                evResp.setStepRatio(1.0);
            }

            List<String> badges = sequenceRuleService.determineTransitionBadges(runningHistory, inv);
            evResp.setBadges(badges);

            eventResponses.add(evResp);
        }

        resp.setEvents(eventResponses);
        return resp;
    }
}
