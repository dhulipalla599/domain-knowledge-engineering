package com.dke.logistics.warehouse_management.api;

import com.dke.logistics.warehouse_management.domain.StockLevel;
import com.dke.logistics.warehouse_management.service.WarehouseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping("/receipts")
    public List<ReceiptResponse> receipts() {
        return service.listReceipts().stream().map(ReceiptResponse::from).toList();
    }

    @PostMapping("/receipts/{id}/receive")
    public ReceiptResponse receive(@PathVariable Long id, @Valid @RequestBody ReceiveRequest request) {
        return ReceiptResponse.from(service.receive(id, request.countedQty()));
    }

    @PostMapping("/receipts/{id}/putaway")
    public ReceiptResponse putAway(@PathVariable Long id, @Valid @RequestBody PutAwayRequest request) {
        return ReceiptResponse.from(service.putAway(id, request.binCode()));
    }

    @GetMapping("/stock")
    public List<StockLevel> stock() {
        return service.listStock();
    }
}
