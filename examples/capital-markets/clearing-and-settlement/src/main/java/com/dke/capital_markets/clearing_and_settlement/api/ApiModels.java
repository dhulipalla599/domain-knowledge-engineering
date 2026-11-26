package com.dke.capital_markets.clearing_and_settlement.api;

import com.dke.capital_markets.clearing_and_settlement.domain.Position;
import com.dke.capital_markets.clearing_and_settlement.domain.Trade;
import com.dke.capital_markets.clearing_and_settlement.domain.TradeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request and response records for the REST API. */
public final class ApiModels {

    private ApiModels() {
    }

    public record CaptureTradeRequest(@NotBlank String tradeId, @NotBlank String buyerAccount,
                                      @NotBlank String sellerAccount, @NotBlank String symbol,
                                      @Positive long quantity, @NotNull @Positive BigDecimal price,
                                      @NotNull LocalDate tradeDate) {
    }

    public record SettlementRunRequest(@NotNull LocalDate settlementDate) {
    }

    public record TradeResponse(String tradeId, String buyerAccount, String sellerAccount, String symbol,
                                long quantity, BigDecimal price, BigDecimal settlementAmount, LocalDate tradeDate,
                                LocalDate settlementDate, TradeStatus status, String failReason) {

        static TradeResponse of(Trade t) {
            return new TradeResponse(t.getTradeId(), t.getBuyerAccount(), t.getSellerAccount(), t.getSymbol(),
                    t.getQuantity(), t.getPrice(), t.settlementAmount(), t.getTradeDate(), t.getSettlementDate(),
                    t.getStatus(), t.getFailReason());
        }
    }

    public record PositionResponse(String accountId, String asset, BigDecimal quantity) {

        static PositionResponse of(Position p) {
            return new PositionResponse(p.getAccountId(), p.getAsset(), p.getQuantity());
        }
    }
}
