package com.dke.retail.store_replenishment.api;

import com.dke.retail.store_replenishment.api.ReplenishmentDtos.ManualOrderRequest;
import com.dke.retail.store_replenishment.api.ReplenishmentDtos.OrderResponse;
import com.dke.retail.store_replenishment.api.ReplenishmentDtos.ReceiveRequest;
import com.dke.retail.store_replenishment.api.ReplenishmentDtos.RunRequest;
import com.dke.retail.store_replenishment.api.ReplenishmentDtos.RunResponse;
import com.dke.retail.store_replenishment.api.ReplenishmentDtos.StoreSkuResponse;
import com.dke.retail.store_replenishment.service.ReplenishmentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReplenishmentController {

    private final ReplenishmentService service;

    public ReplenishmentController(ReplenishmentService service) {
        this.service = service;
    }

    @PostMapping("/replenishment-runs")
    public ResponseEntity<RunResponse> run(@Valid @RequestBody RunRequest request) {
        var result = service.runReplenishment(request.storeId(), request.runDate());
        var status = result.created() > 0 ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(RunResponse.from(result));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeManualOrder(@Valid @RequestBody ManualOrderRequest request) {
        return OrderResponse.from(service.placeManualOrder(request.storeId(), request.sku(), request.quantity()));
    }

    @PostMapping("/orders/{id}/release")
    public OrderResponse release(@PathVariable String id) {
        return OrderResponse.from(service.release(id));
    }

    @PostMapping("/orders/{id}/receive")
    public OrderResponse receive(@PathVariable String id, @Valid @RequestBody ReceiveRequest request) {
        return OrderResponse.from(service.receive(id, request.receivedQuantity()));
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancel(@PathVariable String id) {
        return OrderResponse.from(service.cancel(id));
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders(@RequestParam String storeId) {
        return service.ordersFor(storeId).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/stores/{storeId}/skus/{sku}")
    public StoreSkuResponse storeSku(@PathVariable String storeId, @PathVariable String sku) {
        return StoreSkuResponse.from(service.storeSku(storeId, sku));
    }
}
