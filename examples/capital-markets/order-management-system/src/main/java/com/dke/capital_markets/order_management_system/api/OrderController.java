package com.dke.capital_markets.order_management_system.api;

import com.dke.capital_markets.order_management_system.api.OrderRequests.ExecutionRequest;
import com.dke.capital_markets.order_management_system.api.OrderRequests.OrderResponse;
import com.dke.capital_markets.order_management_system.api.OrderRequests.PlaceOrderRequest;
import com.dke.capital_markets.order_management_system.service.OrderService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest r) {
        var placement = service.place(r.accountId(), r.clientOrderId(), r.symbol(), r.side(), r.quantity(), r.limitPrice());
        HttpStatus status = placement.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OrderResponse.from(placement.order()));
    }

    @PostMapping("/{id}/executions")
    public OrderResponse execution(@PathVariable Long id, @Valid @RequestBody ExecutionRequest r) {
        return OrderResponse.from(service.reportExecution(id, r.quantity(), r.price()));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return OrderResponse.from(service.cancel(id));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(service.get(id));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam String accountId) {
        return service.forAccount(accountId).stream().map(OrderResponse::from).toList();
    }
}
