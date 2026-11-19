package com.sentineltrade.domain.account;

import com.sentineltrade.domain.risk.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/** <<critical>> {integrity}: balances only move through reserve / release / settle. */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "available_balance", nullable = false)
    private BigDecimal availableBalance;

    @Column(name = "reserved_balance", nullable = false)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(name = "daily_risk_limit", nullable = false)
    private BigDecimal dailyRiskLimit;

    @Version
    private Long version;

    protected Account() {
    }

    public Account(Long userId, BigDecimal availableBalance, BigDecimal dailyRiskLimit) {
        this.userId = userId;
        this.availableBalance = availableBalance;
        this.dailyRiskLimit = dailyRiskLimit;
    }

    public void reserve(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        availableBalance = availableBalance.subtract(amount);
        reservedBalance = reservedBalance.add(amount);
    }

    public void release(BigDecimal amount) {
        consumeReserved(amount);
        availableBalance = availableBalance.add(amount);
    }

    /** A purchase was executed: the reserved money is gone. */
    public void settlePurchase(BigDecimal amount) {
        consumeReserved(amount);
    }

    /** A sale was executed: its proceeds become available. */
    public void credit(BigDecimal amount) {
        availableBalance = availableBalance.add(amount);
    }

    private void consumeReserved(BigDecimal amount) {
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Reserved balance would become negative");
        }
        reservedBalance = reservedBalance.subtract(amount);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public BigDecimal getDailyRiskLimit() { return dailyRiskLimit; }
}
