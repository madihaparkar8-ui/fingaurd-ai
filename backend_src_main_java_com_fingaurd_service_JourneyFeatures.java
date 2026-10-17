package com.fingaurd.service;

import java.math.BigDecimal;
import java.util.Map;

public class JourneyFeatures {

    private BigDecimal proposedAmount;
    private BigDecimal firstAmount;
    private BigDecimal lastAmount;
    private BigDecimal historicalMax;
    private BigDecimal baselineMean;
    private double populationStdDev;

    private BigDecimal amountEscalation;
    private double escalationRatio;
    private double historicalMaxRatio;
    private double baselineZScore;
    private double commitmentVelocity; // ₹/day
    private int investmentFrequency30d;
    private double entityConcentration;
    private double maxConsecutiveStepRatio;
    private boolean returnFollowedByEscalation;

    private Map<String, BigDecimal> entityTotals;

    public JourneyFeatures() {}

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public void setProposedAmount(BigDecimal proposedAmount) {
        this.proposedAmount = proposedAmount;
    }

    public BigDecimal getFirstAmount() {
        return firstAmount;
    }

    public void setFirstAmount(BigDecimal firstAmount) {
        this.firstAmount = firstAmount;
    }

    public BigDecimal getLastAmount() {
        return lastAmount;
    }

    public void setLastAmount(BigDecimal lastAmount) {
        this.lastAmount = lastAmount;
    }

    public BigDecimal getHistoricalMax() {
        return historicalMax;
    }

    public void setHistoricalMax(BigDecimal historicalMax) {
        this.historicalMax = historicalMax;
    }

    public BigDecimal getBaselineMean() {
        return baselineMean;
    }

    public void setBaselineMean(BigDecimal baselineMean) {
        this.baselineMean = baselineMean;
    }

    public double getPopulationStdDev() {
        return populationStdDev;
    }

    public void setPopulationStdDev(double populationStdDev) {
        this.populationStdDev = populationStdDev;
    }

    public BigDecimal getAmountEscalation() {
        return amountEscalation;
    }

    public void setAmountEscalation(BigDecimal amountEscalation) {
        this.amountEscalation = amountEscalation;
    }

    public double getEscalationRatio() {
        return escalationRatio;
    }

    public void setEscalationRatio(double escalationRatio) {
        this.escalationRatio = escalationRatio;
    }

    public double getHistoricalMaxRatio() {
        return historicalMaxRatio;
    }

    public void setHistoricalMaxRatio(double historicalMaxRatio) {
        this.historicalMaxRatio = historicalMaxRatio;
    }

    public double getBaselineZScore() {
        return baselineZScore;
    }

    public void setBaselineZScore(double baselineZScore) {
        this.baselineZScore = baselineZScore;
    }

    public double getCommitmentVelocity() {
        return commitmentVelocity;
    }

    public void setCommitmentVelocity(double commitmentVelocity) {
        this.commitmentVelocity = commitmentVelocity;
    }

    public int getInvestmentFrequency30d() {
        return investmentFrequency30d;
    }

    public void setInvestmentFrequency30d(int investmentFrequency30d) {
        this.investmentFrequency30d = investmentFrequency30d;
    }

    public double getEntityConcentration() {
        return entityConcentration;
    }

    public void setEntityConcentration(double entityConcentration) {
        this.entityConcentration = entityConcentration;
    }

    public double getMaxConsecutiveStepRatio() {
        return maxConsecutiveStepRatio;
    }

    public void setMaxConsecutiveStepRatio(double maxConsecutiveStepRatio) {
        this.maxConsecutiveStepRatio = maxConsecutiveStepRatio;
    }

    public boolean isReturnFollowedByEscalation() {
        return returnFollowedByEscalation;
    }

    public void setReturnFollowedByEscalation(boolean returnFollowedByEscalation) {
        this.returnFollowedByEscalation = returnFollowedByEscalation;
    }

    public Map<String, BigDecimal> getEntityTotals() {
        return entityTotals;
    }

    public void setEntityTotals(Map<String, BigDecimal> entityTotals) {
        this.entityTotals = entityTotals;
    }
}
