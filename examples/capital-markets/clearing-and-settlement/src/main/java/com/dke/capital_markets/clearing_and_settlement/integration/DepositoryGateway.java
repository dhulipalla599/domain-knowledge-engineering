package com.dke.capital_markets.clearing_and_settlement.integration;

import com.dke.capital_markets.clearing_and_settlement.domain.Trade;

/**
 * Port to the central securities depository. Delivery versus payment: shares and cash move together or not at all.
 * Production adapter: settlement instructions to the depository (for US equities, DTC via the clearing broker or
 * custodian) over ISO 15022 / ISO 20022 messaging.
 */
public interface DepositoryGateway {

    DvpResult deliverVersusPayment(Trade trade);

    record DvpResult(boolean delivered, String failReason) {

        public static DvpResult ok() {
            return new DvpResult(true, null);
        }

        public static DvpResult failed(String reason) {
            return new DvpResult(false, reason);
        }
    }
}
