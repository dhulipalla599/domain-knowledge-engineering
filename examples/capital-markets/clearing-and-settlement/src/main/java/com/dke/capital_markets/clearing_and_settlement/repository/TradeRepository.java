package com.dke.capital_markets.clearing_and_settlement.repository;

import com.dke.capital_markets.clearing_and_settlement.domain.Trade;
import com.dke.capital_markets.clearing_and_settlement.domain.TradeStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, String> {

    List<Trade> findByStatusInAndSettlementDateLessThanEqualOrderByTradeId(Collection<TradeStatus> statuses, LocalDate date);

    List<Trade> findByStatusInAndSettlementDateOrderByTradeId(Collection<TradeStatus> statuses, LocalDate date);
}
