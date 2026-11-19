package com.sentineltrade.infrastructure.seed;

import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.account.Position;
import com.sentineltrade.domain.user.Role;
import com.sentineltrade.domain.user.User;
import com.sentineltrade.infrastructure.persistence.AccountRepository;
import com.sentineltrade.infrastructure.persistence.PositionRepository;
import com.sentineltrade.infrastructure.persistence.UserRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dev only: creates the trader, auditor and admin demo users on an empty database. */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppProperties.Seed config;
    private final UserRepository users;
    private final AccountRepository accounts;
    private final PositionRepository positions;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(AppProperties properties, UserRepository users, AccountRepository accounts,
                          PositionRepository positions, PasswordEncoder passwordEncoder) {
        this.config = properties.seed();
        this.users = users;
        this.accounts = accounts;
        this.positions = positions;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!config.enabled() || users.count() > 0) {
            return;
        }
        if (isBlank(config.password()) || isBlank(config.mfaSecret())) {
            log.warn("Demo data not created: DEMO_PASSWORD and DEMO_MFA_SECRET must be set");
            return;
        }
        String hash = passwordEncoder.encode(config.password());
        User trader = users.save(new User("trader", hash, config.mfaSecret(), Role.TRADER));
        users.save(new User("auditor", hash, config.mfaSecret(), Role.AUDITOR));
        users.save(new User("admin", hash, config.mfaSecret(), Role.ADMIN));

        Account account = accounts.save(new Account(trader.getId(),
                new BigDecimal("100000.00"), new BigDecimal("50000.00")));
        positions.save(position(account, "PETR4", "1000", "35.00"));
        positions.save(position(account, "VALE3", "500", "60.00"));
        log.info("Demo users created: trader, auditor, admin");
    }

    private static Position position(Account account, String symbol, String quantity, String price) {
        var position = new Position(account.getId(), symbol);
        position.addPurchase(new BigDecimal(quantity), new BigDecimal(price));
        return position;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
