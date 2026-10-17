package com.fingaurd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "investments", indexes = {
    @Index(name = "idx_investment_user_id", columnList = "user_id"),
    @Index(name = "idx_investment_invested_on", columnList = "invested_on")
})
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_id")
    private Long investmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotBlank(message = "Entity name cannot be empty")
    @Column(name = "entity_name", nullable = false, length = 150)
    private String entityName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull(message = "Investment date is required")
    @Column(name = "invested_on", nullable = false)
    private LocalDate investedOn;

    @Column(name = "type", length = 50)
    private String type = "EQUITY";

    @Column(name = "return_amount", precision = 15, scale = 2)
    private BigDecimal returnAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvestmentStatus status = InvestmentStatus.ACTIVE;

    public Investment() {
    }

    public Investment(User user, String entityName, BigDecimal amount, LocalDate investedOn, String type, BigDecimal returnAmount, InvestmentStatus status) {
        this.user = user;
        this.entityName = entityName;
        this.amount = amount;
        this.investedOn = investedOn;
        this.type = type != null ? type : "EQUITY";
        this.returnAmount = returnAmount;
        this.status = status != null ? status : InvestmentStatus.ACTIVE;
    }

    public Long getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(Long investmentId) {
        this.investmentId = investmentId;
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

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getInvestedOn() {
        return investedOn;
    }

    public void setInvestedOn(LocalDate investedOn) {
        this.investedOn = investedOn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getReturnAmount() {
        return returnAmount;
    }

    public void setReturnAmount(BigDecimal returnAmount) {
        this.returnAmount = returnAmount;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(InvestmentStatus status) {
        this.status = status;
    }
}
