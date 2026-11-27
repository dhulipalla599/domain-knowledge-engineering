package com.dke.retail.store_replenishment.api;

import com.dke.retail.store_replenishment.domain.OrderStatus;
import com.dke.retail.store_replenishment.domain.ReplenishmentOrder;
import com.dke.retail.store_replenishment.domain.StoreSku;
import com.dke.retail.store_replenishment.service.ReplenishmentService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class ReplenishmentDtos {

    private ReplenishmentDtos() {
    }

    public record RunRequest(@NotBlank String storeId, @NotNull LocalDate runDate) {
    }

    public record ManualOrderRequest(@NotBlank String storeId, @NotBlank String sku, @NotNull Integer quantity) {
    }

    public record ReceiveRequest(@NotNull @Min(0) Integer receivedQuantity) {
    }

    public record OrderResponse(String id, String storeId, String sku, int quantity, String source,
                                LocalDate runDate, OrderStatus status, String dcOrderRef, Integer receivedQuantity) {
        static OrderResponse from(ReplenishmentOrder o) {
            return new OrderResponse(o.getId(), o.getStoreId(), o.getSku(), o.getQuantity(), o.getSource(),
                    o.getRunDate(), o.getStatus(), o.getDcOrderRef(), o.getReceivedQuantity());
        }
    }

    public record RunResponse(String storeId, LocalDate runDate, int created, List<OrderResponse> orders) {
        static RunResponse from(ReplenishmentService.RunResult r) {
            return new RunResponse(r.storeId(), r.runDate(), r.created(),
                    r.orders().stream().map(OrderResponse::from).toList());
        }
    }

    public record StoreSkuResponse(String storeId, String sku, int onHand, int onOrder, int inventoryPosition,
                                   int reorderPoint, int orderUpTo, int casePack, int shelfCapacity) {
        static StoreSkuResponse from(StoreSku s) {
            return new StoreSkuResponse(s.getStoreId(), s.getSku(), s.getOnHand(), s.getOnOrder(),
                    s.inventoryPosition(), s.getReorderPoint(), s.getOrderUpTo(), s.getCasePack(),
                    s.getShelfCapacity());
        }
    }
}
