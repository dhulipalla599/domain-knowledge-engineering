package com.dke.retail.inventory_management.api;

import com.dke.retail.inventory_management.api.InventoryDtos.ReservationResponse;
import com.dke.retail.inventory_management.api.InventoryDtos.ReserveRequest;
import com.dke.retail.inventory_management.api.InventoryDtos.StockResponse;
import com.dke.retail.inventory_management.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRequest request) {
        var outcome = service.reserve(request.orderRef(), request.sku(), request.quantity());
        // 201 for a new hold, 200 when an idempotent retry returns the existing one
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ReservationResponse.from(outcome.reservation()));
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationResponse confirm(@PathVariable String id) {
        return ReservationResponse.from(service.confirm(id));
    }

    @PostMapping("/reservations/{id}/release")
    public ReservationResponse release(@PathVariable String id) {
        return ReservationResponse.from(service.release(id));
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse get(@PathVariable String id) {
        return ReservationResponse.from(service.get(id));
    }

    @GetMapping("/stock/{sku}")
    public StockResponse stock(@PathVariable String sku) {
        return StockResponse.from(service.stockFor(sku));
    }
}
