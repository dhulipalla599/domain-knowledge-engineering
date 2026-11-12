package com.dke.retail.order_management_and_fulfillment.api;

import com.dke.retail.order_management_and_fulfillment.api.OrderDtos.InventoryResponse;
import com.dke.retail.order_management_and_fulfillment.api.OrderDtos.OrderResponse;
import com.dke.retail.order_management_and_fulfillment.api.OrderDtos.PlaceOrderRequest;
import com.dke.retail.order_management_and_fulfillment.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** Checkout calls this once per order; retries must reuse the same Idempotency-Key. */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                    @Valid @RequestBody PlaceOrderRequest request) {
        OrderService.PlaceOrderResult result = orders.placeOrder(idempotencyKey, request.toCommand());
        OrderResponse body = OrderResponse.from(result.order());
        return result.created()
                ? ResponseEntity.created(URI.create("/api/orders/" + body.orderNumber())).body(body)
                : ResponseEntity.ok(body);
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderResponse get(@PathVariable String orderNumber) {
        return OrderResponse.from(orders.get(orderNumber));
    }

    @PostMapping("/orders/{orderNumber}/ship")
    public OrderResponse ship(@PathVariable String orderNumber) {
        return OrderResponse.from(orders.ship(orderNumber));
    }

    @PostMapping("/orders/{orderNumber}/deliver")
    public OrderResponse deliver(@PathVariable String orderNumber) {
        return OrderResponse.from(orders.markDelivered(orderNumber));
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderResponse cancel(@PathVariable String orderNumber,
                                @RequestParam(defaultValue = "CUSTOMER_REQUEST") String reason) {
        return OrderResponse.from(orders.cancel(orderNumber, reason));
    }

    @GetMapping("/inventory/{sku}")
    public List<InventoryResponse> inventory(@PathVariable String sku) {
        return orders.inventoryFor(sku).stream().map(InventoryResponse::from).toList();
    }
}
