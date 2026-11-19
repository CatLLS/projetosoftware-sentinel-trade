package com.sentineltrade.domain.risk;

import com.sentineltrade.domain.order.Order;
import org.springframework.stereotype.Component;

/** The notional traded today, including this order, must stay within the daily risk limit. */
@Component
public class OrderExposureLimitRule implements RiskRule {

    @Override
    public void check(Order order, RiskContext context) {
        var exposure = context.tradedToday().add(order.getNotional());
        if (exposure.compareTo(context.account().getDailyRiskLimit()) > 0) {
            throw new RiskLimitExceededException();
        }
    }
}
