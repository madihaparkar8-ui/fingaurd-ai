package com.fingaurd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journeys", indexes = {
    @Index(name = "idx_journey_user_id", columnList = "user_id")
})
public class Journey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journey_id")
    private Long journeyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount = 0;

    @Column(name = "total_exposure", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalExposure = BigDecimal.ZERO;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<RiskReport> riskReports = new ArrayList<>();

    public Journey() {
    }

    public Journey(User user, LocalDate startDate, LocalDate endDate, Integer eventCount, BigDecimal totalExposure) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventCount = eventCount != null ? eventCount : 0;
        this.totalExposure = totalExposure != null ? totalExposure : BigDecimal.ZERO;
    }

    public Long getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(Long journeyId) {
        this.journeyId = journeyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public BigDecimal getTotalExposure() {
        return totalExposure;
    }

    public void setTotalExposure(BigDecimal totalExposure) {
        this.totalExposure = totalExposure;
    }

    public List<RiskReport> getRiskReports() {
        return riskReports;
    }

    public void setRiskReports(List<RiskReport> riskReports) {
        this.riskReports = riskReports;
    }
}
