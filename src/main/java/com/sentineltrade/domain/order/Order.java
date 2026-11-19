package com.sentineltrade.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * <<critical>> aggregate. The status only changes through business methods, which
 * enforce the state machine of {@link OrderStatus}; there are no public setters.
 */
@Entity
@Table(name = "orders")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "order_type")
public abstract class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "asset_symbol", nullable = false, updatable = false)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrderSide side;

    @Column(nullable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "executed_quantity", nullable = false)
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    /** Price used to reserve funds and settle fills. */
    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = createdAt;

    @Version
    private Long version;

    protected Order() {
    }

    protected Order(String idempotencyKey, Long accountId, String assetSymbol,
                    OrderSide side, BigDecimal quantity, BigDecimal price) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.assetSymbol = assetSymbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    public abstract OrderType getType();

    // --- State machine -------------------------------------------------------

    public void validate() {
        transitionTo(OrderStatus.VALIDATED);
    }

    public void reject(String reason) {
        transitionTo(OrderStatus.REJECTED);
        this.rejectionReason = reason;
    }

    public void markSent() {
        transitionTo(OrderStatus.SENT);
    }

    /** The exchange acknowledged the order. */
    public void accept() {
        transitionTo(OrderStatus.OPEN);
    }

    /** No conclusive answer from the exchange: never resend, reconcile instead. */
    public void markPendingReconciliation() {
        transitionTo(OrderStatus.PENDING_RECONCILIATION);
    }

    public void registerExecution(BigDecimal filled) {
        if (filled.signum() <= 0 || filled.compareTo(getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException("Invalid execution quantity");
        }
        if (filled.compareTo(getRemainingQuantity()) == 0) {
            transitionTo(OrderStatus.EXECUTED);
        } else if (status != OrderStatus.CANCEL_REQUESTED) {
            // a partial fill while cancelling keeps waiting for the cancel confirmation
            transitionTo(OrderStatus.PARTIALLY_EXECUTED);
        }
        executedQuantity = executedQuantity.add(filled);
        updatedAt = Instant.now();
    }

    public void requestCancel() {
        transitionTo(OrderStatus.CANCEL_REQUESTED);
    }

    public void confirmCancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public void expire() {
        transitionTo(OrderStatus.EXPIRED);
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStateException(status, target);
        }
        status = target;
        updatedAt = Instant.now();
    }

    /** Only subclasses that allow repricing (limit orders) may change the price. */
    protected void reprice(BigDecimal newPrice) {
        this.price = newPrice;
        this.updatedAt = Instant.now();
    }

    // --- Derived values ------------------------------------------------------

    public BigDecimal amountFor(BigDecimal qty) {
        return qty.multiply(price).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getNotional() {
        return amountFor(quantity);
    }

    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(executedQuantity);
    }

    // --- Getters -------------------------------------------------------------

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getAccountId() { return accountId; }
    public String getAssetSymbol() { return assetSymbol; }
    public OrderSide getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public BigDecimal getPrice() { return price; }
    public OrderStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
