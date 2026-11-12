package com.dke.retail.order_management_and_fulfillment.api;

import com.dke.retail.order_management_and_fulfillment.domain.CustomerOrder;
import com.dke.retail.order_management_and_fulfillment.domain.InventoryPosition;
import com.dke.retail.order_management_and_fulfillment.domain.OrderLine;
import com.dke.retail.order_management_and_fulfillment.service.OrderService.PlaceOrderCommand;
import com.dke.retail.order_management_and_fulfillment.service.SourcingService.LineRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/** Request and response records for the order API. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record LineItem(@NotBlank String sku, @Positive int quantity) {
    }

    public record PlaceOrderRequest(@NotBlank String customerId, @NotBlank String shipToRegion,
                                    @NotBlank String paymentToken, @NotEmpty List<@Valid LineItem> lines) {
        PlaceOrderCommand toCommand() {
            return new PlaceOrderCommand(customerId, shipToRegion, paymentToken,
                    lines.stream().map(l -> new LineRequest(l.sku(), l.quantity())).toList());
        }
    }

    public record LineResponse(String sku, int quantity, BigDecimal unitPrice, String fulfillmentNode,
                               String trackingNumber) {
        static LineResponse from(OrderLine l) {
            return new LineResponse(l.getSku(), l.getQuantity(), l.getUnitPrice(), l.getFulfillmentNode(),
                    l.getTrackingNumber());
        }
    }

    public record OrderResponse(String orderNumber, String status, String customerId, BigDecimal totalAmount,
                                String currency, List<LineResponse> lines) {
        static OrderResponse from(CustomerOrder o) {
            return new OrderResponse(o.getOrderNumber(), o.getStatus().name(), o.getCustomerId(), o.getTotalAmount(),
                    o.getCurrency(), o.getLines().stream().map(LineResponse::from).toList());
        }
    }

    public record InventoryResponse(String nodeCode, String sku, int onHand, int reserved, int available) {
        static InventoryResponse from(InventoryPosition p) {
            return new InventoryResponse(p.getNodeCode(), p.getSku(), p.getOnHand(), p.getReserved(), p.available());
        }
    }
}
