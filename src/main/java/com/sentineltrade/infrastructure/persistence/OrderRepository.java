package com.sentineltrade.infrastructure.persistence;

import com.sentineltrade.domain.order.Order;
import com.sentineltrade.domain.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdAndAccountId(Long id, Long accountId);

    /** Any filter left {@code null} is ignored. */
    @Query("""
            select o from Order o
            where (:accountId is null or o.accountId = :accountId)
              and (:status is null or o.status = :status)
              and (:assetSymbol is null or o.assetSymbol = :assetSymbol)
            """)
    Page<Order> search(Long accountId, OrderStatus status, String assetSymbol, Pageable pageable);

    @Query("""
            select sum(o.quantity * o.price) from Order o
            where o.accountId = :accountId and o.createdAt >= :since
              and o.status <> com.sentineltrade.domain.order.OrderStatus.REJECTED
            """)
    /** @return {@code null} when there is no such order */
    BigDecimal notionalPlacedSince(Long accountId, Instant since);

    List<Order> findByStatusInAndUpdatedAtBefore(Collection<OrderStatus> statuses, Instant before);
}
