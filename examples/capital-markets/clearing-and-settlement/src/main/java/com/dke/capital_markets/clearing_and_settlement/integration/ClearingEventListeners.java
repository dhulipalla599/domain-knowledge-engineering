package com.dke.capital_markets.clearing_and_settlement.integration;

import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.SettlementFailed;
import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.TradeAffirmed;
import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.TradeCaptured;
import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.TradeSettled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-ins for Kafka consumers: client confirmations, books and records, and the fails desk. */
@Component
public class ClearingEventListeners {

    private static final Logger log = LoggerFactory.getLogger(ClearingEventListeners.class);

    @EventListener
    public void onCaptured(TradeCaptured e) {
        log.info("[clearing.trade-captured.v1] {} {} x {} @ {} settles {}", e.tradeId(), e.symbol(), e.quantity(),
                e.price(), e.settlementDate());
    }

    @EventListener
    public void onAffirmed(TradeAffirmed e) {
        log.info("[clearing.trade-affirmed.v1] {} eligible for settlement on {}", e.tradeId(), e.settlementDate());
    }

    @EventListener
    public void onSettled(TradeSettled e) {
        log.info("[settlement.trade-settled.v1] {} booked to stock record: {} x {} for {}", e.tradeId(), e.symbol(),
                e.quantity(), e.amount());
    }

    @EventListener
    public void onFailed(SettlementFailed e) {
        log.warn("[settlement.trade-failed.v1] {} failed on {}: {} - opened on fails desk", e.tradeId(),
                e.settlementDate(), e.reason());
    }
}
