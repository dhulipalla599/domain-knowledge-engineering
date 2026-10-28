package com.dke.capital_markets.order_management_system.api;

import com.dke.capital_markets.order_management_system.domain.Order;
import com.dke.capital_markets.order_management_system.domain.OrderStatus;
import com.dke.capital_markets.order_management_system.domain.Side;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class OrderRequests {

    private OrderRequests() {
    }

    public record PlaceOrderRequest(@NotBlank String accountId, @NotBlank String clientOrderId, @NotBlank String symbol,
                                    @NotNull Side side, long quantity, @NotNull BigDecimal limitPrice) {
    }

    public record ExecutionRequest(long quantity, @NotNull BigDecimal price) {
    }

    public record OrderResponse(Long id, String accountId, String clientOrderId, String symbol, Side side, long quantity,
                                BigDecimal limitPrice, long filledQuantity, OrderStatus status) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getAccountId(), o.getClientOrderId(), o.getSymbol(), o.getSide(),
                    o.getQuantity(), o.getLimitPrice(), o.getFilledQuantity(), o.getStatus());
        }
    }
}
