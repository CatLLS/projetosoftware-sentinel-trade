package com.sentineltrade.domain.account;

import com.sentineltrade.domain.risk.InsufficientPositionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** <<critical>> {integrity}: custody of one asset for one account. */
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "asset_symbol", nullable = false, updatable = false)
    private String assetSymbol;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "average_price", nullable = false)
    private BigDecimal averagePrice = BigDecimal.ZERO;

    @Version
    private Long version;

    protected Position() {
    }

    public Position(Long accountId, String assetSymbol) {
        this.accountId = accountId;
        this.assetSymbol = assetSymbol;
    }

    public BigDecimal getFreeQuantity() {
        return quantity.subtract(reservedQuantity);
    }

    public void reserve(BigDecimal qty) {
        if (getFreeQuantity().compareTo(qty) < 0) {
            throw new InsufficientPositionException();
        }
        reservedQuantity = reservedQuantity.add(qty);
    }

    public void release(BigDecimal qty) {
        if (reservedQuantity.compareTo(qty) < 0) {
            throw new IllegalStateException("Reserved quantity would become negative");
        }
        reservedQuantity = reservedQuantity.subtract(qty);
    }

    public void addPurchase(BigDecimal qty, BigDecimal price) {
        BigDecimal newQuantity = quantity.add(qty);
        averagePrice = quantity.multiply(averagePrice).add(qty.multiply(price))
                .divide(newQuantity, 4, RoundingMode.HALF_UP);
        quantity = newQuantity;
    }

    /** A sale was executed: the reserved quantity leaves custody. */
    public void removeSale(BigDecimal qty) {
        release(qty);
        quantity = quantity.subtract(qty);
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public String getAssetSymbol() { return assetSymbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
}
