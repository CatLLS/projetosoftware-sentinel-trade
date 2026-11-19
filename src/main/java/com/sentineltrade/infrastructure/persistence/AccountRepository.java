package com.sentineltrade.infrastructure.persistence;

import com.sentineltrade.domain.account.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);
}
