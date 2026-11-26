package com.dke.capital_markets.clearing_and_settlement.integration;

import com.dke.capital_markets.clearing_and_settlement.domain.Position;
import com.dke.capital_markets.clearing_and_settlement.domain.Trade;
import com.dke.capital_markets.clearing_and_settlement.repository.PositionRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** In-memory stand-in for the depository: positions live in the local database. */
@Component
public class LedgerDepositoryGateway implements DepositoryGateway {

    private final PositionRepository positions;

    public LedgerDepositoryGateway(PositionRepository positions) {
        this.positions = positions;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public DvpResult deliverVersusPayment(Trade trade) {
        BigDecimal shares = BigDecimal.valueOf(trade.getQuantity());
        BigDecimal cash = trade.settlementAmount();
        Position sellerShares = position(trade.getSellerAccount(), trade.getSymbol());
        Position buyerCash = position(trade.getBuyerAccount(), Position.CASH);
        if (!sellerShares.covers(shares)) {
            return DvpResult.failed("INSUFFICIENT_SECURITIES");
        }
        if (!buyerCash.covers(cash)) {
            return DvpResult.failed("INSUFFICIENT_CASH");
        }
        sellerShares.debit(shares);
        position(trade.getBuyerAccount(), trade.getSymbol()).credit(shares);
        buyerCash.debit(cash);
        position(trade.getSellerAccount(), Position.CASH).credit(cash);
        return DvpResult.ok();
    }

    private Position position(String accountId, String asset) {
        return positions.findById(Position.key(accountId, asset))
                .orElseGet(() -> positions.save(new Position(accountId, asset, BigDecimal.ZERO)));
    }
}
