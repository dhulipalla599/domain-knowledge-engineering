package com.dke.capital_markets.clearing_and_settlement.api;

import com.dke.capital_markets.clearing_and_settlement.api.ApiModels.CaptureTradeRequest;
import com.dke.capital_markets.clearing_and_settlement.api.ApiModels.PositionResponse;
import com.dke.capital_markets.clearing_and_settlement.api.ApiModels.SettlementRunRequest;
import com.dke.capital_markets.clearing_and_settlement.api.ApiModels.TradeResponse;
import com.dke.capital_markets.clearing_and_settlement.repository.PositionRepository;
import com.dke.capital_markets.clearing_and_settlement.service.ClearingService;
import com.dke.capital_markets.clearing_and_settlement.service.ClearingService.NetObligation;
import com.dke.capital_markets.clearing_and_settlement.service.ClearingService.SettlementRun;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ClearingController {

    private final ClearingService clearing;
    private final PositionRepository positions;

    public ClearingController(ClearingService clearing, PositionRepository positions) {
        this.clearing = clearing;
        this.positions = positions;
    }

    @PostMapping("/trades")
    public ResponseEntity<TradeResponse> capture(@Valid @RequestBody CaptureTradeRequest r) {
        var capture = clearing.capture(r.tradeId(), r.buyerAccount(), r.sellerAccount(), r.symbol(), r.quantity(),
                r.price(), r.tradeDate());
        return ResponseEntity.status(capture.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(TradeResponse.of(capture.trade()));
    }

    @GetMapping("/trades/{tradeId}")
    public TradeResponse get(@PathVariable String tradeId) {
        return TradeResponse.of(clearing.get(tradeId));
    }

    @PostMapping("/trades/{tradeId}/affirm")
    public TradeResponse affirm(@PathVariable String tradeId) {
        return TradeResponse.of(clearing.affirm(tradeId));
    }

    @PostMapping("/trades/{tradeId}/cancel")
    public TradeResponse cancel(@PathVariable String tradeId) {
        return TradeResponse.of(clearing.cancel(tradeId));
    }

    @PostMapping("/settlement-runs")
    public SettlementRun runSettlement(@Valid @RequestBody SettlementRunRequest r) {
        return clearing.runSettlement(r.settlementDate());
    }

    @GetMapping("/obligations")
    public List<NetObligation> obligations(@RequestParam LocalDate settlementDate) {
        return clearing.netObligations(settlementDate);
    }

    @GetMapping("/accounts/{accountId}/positions")
    public List<PositionResponse> positions(@PathVariable String accountId) {
        return positions.findByAccountIdOrderByAsset(accountId).stream().map(PositionResponse::of).toList();
    }
}
