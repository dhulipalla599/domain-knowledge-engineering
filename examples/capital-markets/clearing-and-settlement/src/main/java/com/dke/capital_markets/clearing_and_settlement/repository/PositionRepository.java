package com.dke.capital_markets.clearing_and_settlement.repository;

import com.dke.capital_markets.clearing_and_settlement.domain.Position;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, String> {

    List<Position> findByAccountIdOrderByAsset(String accountId);
}
