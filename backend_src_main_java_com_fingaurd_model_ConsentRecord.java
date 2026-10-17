package com.fingaurd.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consent_records", indexes = {
    @Index(name = "idx_consent_user_id", columnList = "user_id")
})
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Long consentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose = "COMMUNICATION_ANALYSIS";

    @Column(name = "granted", nullable = false)
    private Boolean granted = false;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public ConsentRecord() {
    }

    public ConsentRecord(Long userId, String purpose, Boolean granted, LocalDateTime timestamp) {
        this.userId = userId;
        this.purpose = purpose != null ? purpose : "COMMUNICATION_ANALYSIS";
        this.granted = granted != null ? granted : false;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public Long getConsentId() {
        return consentId;
    }

    public void setConsentId(Long consentId) {
        this.consentId = consentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
