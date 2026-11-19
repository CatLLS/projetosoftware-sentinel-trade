package com.sentineltrade.application.order;

import com.sentineltrade.config.AppProperties;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically asks the exchange about every live order: picks up fills of open orders,
 * resolves timeouts (PENDING_RECONCILIATION) and retries unconfirmed cancellations.
 */
@Component
public class OrderReconciler {

    private static final Logger log = LoggerFactory.getLogger(OrderReconciler.class);

    private final OrderService orderService;
    private final AppProperties.Reconciler config;

    public OrderReconciler(OrderService orderService, AppProperties properties) {
        this.orderService = orderService;
        this.config = properties.reconciler();
    }

    @Scheduled(fixedDelayString = "${app.reconciler.interval}",
               initialDelayString = "${app.reconciler.interval}")
    public void reconcileLiveOrders() {
        // the grace period leaves orders that are being handled by a request alone
        for (Long orderId : orderService.liveOrderIds(Instant.now().minus(config.grace()))) {
            try {
                orderService.reconcile(orderId);
            } catch (RuntimeException e) {
                log.warn("Reconciliation of order {} failed: {}", orderId, e.getClass().getSimpleName());
            }
        }
    }
}
