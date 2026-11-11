package com.dke.capital_markets.trade_execution_and_smart_order_routing.api;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.api.ExecutionDtos.OrderResponse;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.api.ExecutionDtos.SubmitOrderRequest;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.MarketDataProvider.VenueQuote;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.service.ExecutionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final ExecutionService service;

    public ExecutionController(ExecutionService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> submit(@Valid @RequestBody SubmitOrderRequest req) {
        var order = service.submit(req.clientOrderId(), req.accountId(), req.symbol().toUpperCase(),
                req.side(), req.quantity(), req.limitPrice());
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId())).body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(service.get(id));
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return OrderResponse.from(service.cancel(id));
    }

    @GetMapping("/market/{symbol}/quotes")
    public List<VenueQuote> quotes(@PathVariable String symbol) {
        return service.book(symbol.toUpperCase());
    }
}
