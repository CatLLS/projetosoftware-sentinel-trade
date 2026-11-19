package com.sentineltrade.infrastructure.persistence;

import com.sentineltrade.domain.account.Position;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByAccountIdAndAssetSymbol(Long accountId, String assetSymbol);
}
