package com.sentineltrade.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentineltrade.domain.risk.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void initializesWithZeroReservedBalance() {
        Account account = new Account(42L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));

        assertThat(account.getUserId()).isEqualTo(42L);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("1000.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getDailyRiskLimit()).isEqualByComparingTo("5000.00");
    }

    @Test
    void reserveMovesMoneyFromAvailableToReserved() {
        Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));

        account.reserve(new BigDecimal("350.00"));

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("650.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("350.00");
    }

    @Test
    void reserveRejectsAmountAboveAvailableBalance() {
        Account account = new Account(1L, new BigDecimal("100.00"), new BigDecimal("5000.00"));

        assertThatThrownBy(() -> account.reserve(new BigDecimal("100.01")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("100.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void releaseMovesMoneyBackFromReservedToAvailable() {
        Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));
        account.reserve(new BigDecimal("400.00"));

        account.release(new BigDecimal("150.00"));

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("750.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("250.00");
    }

    @Test
    void releaseAboveReservedThrowsIllegalState() {
        Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));
        account.reserve(new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.release(new BigDecimal("100.01")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reserved balance would become negative");
    }

    @Test
    void settlePurchaseConsumesReservedWithoutRestoringAvailable() {
        Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));
        account.reserve(new BigDecimal("400.00"));

        account.settlePurchase(new BigDecimal("400.00"));

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("600.00");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void settlePurchaseAboveReservedThrowsIllegalState() {
        Account account = new Account(1L, new BigDecimal("1000.00"), new BigDecimal("5000.00"));

        assertThatThrownBy(() -> account.settlePurchase(new BigDecimal("10.00")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void creditAddsSaleProceedsToAvailableBalance() {
        Account account = new Account(1L, new BigDecimal("500.00"), new BigDecimal("5000.00"));

        account.credit(new BigDecimal("250.50"));

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("750.50");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
    }
}
