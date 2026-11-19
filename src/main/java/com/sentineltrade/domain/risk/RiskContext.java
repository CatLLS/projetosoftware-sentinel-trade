package com.sentineltrade.domain.risk;

import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.account.Position;
import java.math.BigDecimal;

/**
 * Everything a rule needs to decide, loaded once by the application layer.
 *
 * @param position     the account's position in the order's asset, or {@code null}
 * @param tradedToday  notional of the account's non-rejected orders placed today
 */
public record RiskContext(Account account, Position position, BigDecimal tradedToday) {
}
