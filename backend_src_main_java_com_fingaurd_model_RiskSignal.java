package com.fingaurd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "risk_signals", indexes = {
    @Index(name = "idx_signal_report_id", columnList = "report_id")
})
public class RiskSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signal_id")
    private Long signalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private RiskReport riskReport;

    @Column(name = "type", nullable = false, length = 80)
    private String type; // e.g. ESCALATION_RATIO, BASELINE_ZSCORE, VELOCITY, SEQUENCE_RULE, INFLUENCE_INDICATOR

    @Column(name = "signal_value", length = 100)
    private String value;

    @Column(name = "explanation", length = 1000, nullable = false)
    private String explanation;

    @Column(name = "source_event")
    private Long sourceEvent; // optional ID of triggering investment event

    public RiskSignal() {
    }

    public RiskSignal(String type, String value, String explanation, Long sourceEvent) {
        this.type = type;
        this.value = value;
        this.explanation = explanation;
        this.sourceEvent = sourceEvent;
    }

    public Long getSignalId() {
        return signalId;
    }

    public void setSignalId(Long signalId) {
        this.signalId = signalId;
    }

    public RiskReport getRiskReport() {
        return riskReport;
    }

    public void setRiskReport(RiskReport riskReport) {
        this.riskReport = riskReport;
    }

    public Long getReportId() {
        return riskReport != null ? riskReport.getReportId() : null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Long getSourceEvent() {
        return sourceEvent;
    }

    public void setSourceEvent(Long sourceEvent) {
        this.sourceEvent = sourceEvent;
    }
}
