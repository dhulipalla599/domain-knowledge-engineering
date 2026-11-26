package com.dke.capital_markets.clearing_and_settlement.service;

import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.SettlementFailed;
import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.TradeAffirmed;
import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.TradeCaptured;
import com.dke.capital_markets.clearing_and_settlement.domain.ClearingEvents.TradeSettled;
import com.dke.capital_markets.clearing_and_settlement.domain.Position;
import com.dke.capital_markets.clearing_and_settlement.domain.Trade;
import com.dke.capital_markets.clearing_and_settlement.domain.TradeStatus;
import com.dke.capital_markets.clearing_and_settlement.integration.DepositoryGateway;
import com.dke.capital_markets.clearing_and_settlement.integration.DepositoryGateway.DvpResult;
import com.dke.capital_markets.clearing_and_settlement.repository.TradeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClearingService {

    private static final int SETTLEMENT_CYCLE_DAYS = 1; // T+1
    private static final Set<TradeStatus> SETTLEABLE = Set.of(TradeStatus.AFFIRMED, TradeStatus.FAILED);

    private final TradeRepository trades;
    private final DepositoryGateway depository;
    private final SettlementCalendar calendar;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ClearingService(TradeRepository trades, DepositoryGateway depository, SettlementCalendar calendar,
                           ApplicationEventPublisher events, Clock clock) {
        this.trades = trades;
        this.depository = depository;
        this.calendar = calendar;
        this.events = events;
        this.clock = clock;
    }

    public record Capture(Trade trade, boolean created) {
    }

    public record SettlementRun(LocalDate settlementDate, List<String> settled, List<String> failed) {
    }

    public record NetObligation(String accountId, String asset, BigDecimal netQuantity) {
    }

    /** Captures an executed trade. Replaying the same tradeId returns the original (created=false). */
    @Transactional
    public Capture capture(String tradeId, String buyer, String seller, String symbol, long quantity,
                           BigDecimal price, LocalDate tradeDate) {
        var existing = trades.findById(tradeId);
        if (existing.isPresent()) {
            return new Capture(existing.get(), false);
        }
        if (buyer.equals(seller)) {
            throw new RuleViolationException("SAME_PARTY", "buyer and seller must be different accounts");
        }
        if (!calendar.isBusinessDay(tradeDate) || tradeDate.isAfter(today())) {
            throw new RuleViolationException("TRADE_DATE", tradeDate + " is not a past or current business day");
        }
        LocalDate settlementDate = calendar.settlementDate(tradeDate, SETTLEMENT_CYCLE_DAYS);
        Trade trade = trades.save(new Trade(tradeId, buyer, seller, symbol, quantity, price, tradeDate, settlementDate));
        events.publishEvent(new TradeCaptured(tradeId, buyer, seller, symbol, quantity, price, settlementDate));
        return new Capture(trade, true);
    }

    /** Institutional affirmation must happen by 9:00 pm New York time on trade date. */
    @Transactional
    public Trade affirm(String tradeId) {
        Trade trade = get(tradeId);
        var cutoff = calendar.affirmationCutoff(trade.getTradeDate());
        if (clock.instant().isAfter(cutoff.toInstant())) {
            throw new RuleViolationException("AFFIRMATION_CUTOFF",
                    "affirmation cutoff was " + cutoff + "; route trade " + tradeId + " to the exceptions desk");
        }
        trade.affirm();
        events.publishEvent(new TradeAffirmed(tradeId, trade.getSettlementDate()));
        return trade;
    }

    @Transactional
    public Trade cancel(String tradeId) {
        Trade trade = get(tradeId);
        trade.cancel();
        return trade;
    }

    /** Settles every open obligation due on or before the date, delivery versus payment, trade by trade. */
    @Transactional
    public SettlementRun runSettlement(LocalDate settlementDate) {
        if (!calendar.isBusinessDay(settlementDate) || settlementDate.isAfter(today())) {
            throw new RuleViolationException("SETTLEMENT_DATE",
                    settlementDate + " is not a business day on or before " + today());
        }
        List<String> settled = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Trade trade : trades.findByStatusInAndSettlementDateLessThanEqualOrderByTradeId(SETTLEABLE, settlementDate)) {
            DvpResult result = depository.deliverVersusPayment(trade);
            if (result.delivered()) {
                trade.settle(clock.instant());
                settled.add(trade.getTradeId());
                events.publishEvent(new TradeSettled(trade.getTradeId(), trade.getSymbol(), trade.getQuantity(),
                        trade.settlementAmount(), settlementDate));
            } else {
                trade.fail(result.failReason());
                failed.add(trade.getTradeId());
                events.publishEvent(new SettlementFailed(trade.getTradeId(), result.failReason(), settlementDate));
            }
        }
        return new SettlementRun(settlementDate, settled, failed);
    }

    /** Multilateral net: per account, the shares to receive (+) or deliver (-) and the cash to receive (+) or pay (-). */
    @Transactional(readOnly = true)
    public List<NetObligation> netObligations(LocalDate settlementDate) {
        Map<String, BigDecimal> net = new TreeMap<>();
        for (Trade t : trades.findByStatusInAndSettlementDateOrderByTradeId(SETTLEABLE, settlementDate)) {
            BigDecimal shares = BigDecimal.valueOf(t.getQuantity());
            net.merge(t.getBuyerAccount() + "|" + t.getSymbol(), shares, BigDecimal::add);
            net.merge(t.getSellerAccount() + "|" + t.getSymbol(), shares.negate(), BigDecimal::add);
            net.merge(t.getBuyerAccount() + "|" + Position.CASH, t.settlementAmount().negate(), BigDecimal::add);
            net.merge(t.getSellerAccount() + "|" + Position.CASH, t.settlementAmount(), BigDecimal::add);
        }
        return net.entrySet().stream()
                .map(e -> new NetObligation(e.getKey().split("\\|")[0], e.getKey().split("\\|")[1], e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Trade get(String tradeId) {
        return trades.findById(tradeId).orElseThrow(() -> new NotFoundException("trade " + tradeId + " not found"));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(SettlementCalendar.MARKET_ZONE));
    }
}
