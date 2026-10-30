package com.dke.retail.inventory_management.api;

import com.dke.retail.inventory_management.domain.Reservation;
import com.dke.retail.inventory_management.domain.ReservationStatus;
import com.dke.retail.inventory_management.domain.StockItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record ReserveRequest(@NotBlank String orderRef, @NotBlank String sku, @Min(1) int quantity) {
    }

    public record ReservationResponse(String id, String orderRef, String sku, int quantity,
                                      ReservationStatus status, Instant expiresAt) {
        static ReservationResponse from(Reservation r) {
            return new ReservationResponse(r.getId(), r.getOrderRef(), r.getSku(), r.getQuantity(),
                    r.getStatus(), r.getExpiresAt());
        }
    }

    public record StockResponse(String sku, String name, int onHand, int reserved, int available, int reorderPoint) {
        static StockResponse from(StockItem s) {
            return new StockResponse(s.getSku(), s.getName(), s.getOnHand(), s.getReserved(),
                    s.available(), s.getReorderPoint());
        }
    }
}
