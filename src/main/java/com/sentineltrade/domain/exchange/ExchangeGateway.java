package com.sentineltrade.domain.exchange;

import com.sentineltrade.domain.order.Order;
import java.math.BigDecimal;

/**
 * Port to the exchange. Every call may throw {@link ExchangeTimeoutException}, meaning the
 * outcome is unknown. Only {@link #submit} is unsafe to retry; the others are idempotent.
 */
public interface ExchangeGateway {

    ExchangeReport submit(Order order);

    ExchangeReport query(Long orderId);

    ExchangeReport cancel(Long orderId);

    ExchangeReport amend(Long orderId, BigDecimal newPrice);
}
