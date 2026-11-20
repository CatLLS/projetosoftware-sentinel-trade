package com.sentineltrade.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentineltrade.config.AppProperties;
import com.sentineltrade.domain.account.Account;
import com.sentineltrade.domain.account.Position;
import com.sentineltrade.domain.user.User;
import com.sentineltrade.infrastructure.persistence.AccountRepository;
import com.sentineltrade.infrastructure.persistence.PositionRepository;
import com.sentineltrade.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class DemoDataSeederTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PositionRepository positions = mock(PositionRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private DemoDataSeeder seeder(boolean enabled, String password, String mfaSecret) {
        AppProperties props = new AppProperties(null, null, null, null,
                new AppProperties.Seed(enabled, password, mfaSecret));
        return new DemoDataSeeder(props, users, accounts, positions, encoder);
    }

    @Test
    void seedsDemoUsersAccountAndPositionsOnEmptyDatabase() {
        when(users.count()).thenReturn(0L);
        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        });
        when(accounts.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 10L);
            return a;
        });

        seeder(true, "DemoPass123", "JBSWY3DPEHPK3PXP").run(new DefaultApplicationArguments());

        ArgumentCaptor<User> savedUsers = ArgumentCaptor.forClass(User.class);
        verify(users, times(3)).save(savedUsers.capture());
        assertThat(savedUsers.getAllValues())
                .extracting(User::getUsername)
                .containsExactly("trader", "auditor", "admin");

        verify(accounts, times(1)).save(any(Account.class));
        verify(positions, times(2)).save(any(Position.class));
    }

    @Test
    void skipsSeedingWhenDisabledAlreadyPopulatedOrMissingSecrets() {
        seeder(false, "DemoPass123", "JBSWY3DPEHPK3PXP").run(new DefaultApplicationArguments());
        verify(users, never()).save(any());

        when(users.count()).thenReturn(3L);
        seeder(true, "DemoPass123", "JBSWY3DPEHPK3PXP").run(new DefaultApplicationArguments());
        verify(users, never()).save(any());

        when(users.count()).thenReturn(0L);
        seeder(true, "", "JBSWY3DPEHPK3PXP").run(new DefaultApplicationArguments());
        seeder(true, "DemoPass123", "  ").run(new DefaultApplicationArguments());
        verify(users, never()).save(any());
    }
}
