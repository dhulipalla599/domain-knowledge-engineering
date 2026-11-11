package com.dke.capital_markets.trade_execution_and_smart_order_routing.api;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ChildOrder;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ParentOrder;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Side;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public final class ExecutionDtos {

    private ExecutionDtos() {
    }

    public record SubmitOrderRequest(
            @NotBlank String clientOrderId,
            @NotBlank String accountId,
            @NotBlank String symbol,
            @NotNull Side side,
            @Positive long quantity,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal limitPrice) {
    }

    public record ChildOrderResponse(Long id, String venue, long quantity, BigDecimal limitPrice,
                                     long filledQuantity, BigDecimal fillPrice, String status) {
        static ChildOrderResponse from(ChildOrder c) {
            return new ChildOrderResponse(c.getId(), c.getVenueCode(), c.getQuantity(), c.getLimitPrice(),
                    c.getFilledQuantity(), c.getFillPrice(), c.getStatus().name());
        }
    }

    public record OrderResponse(Long id, String clientOrderId, String symbol, Side side, long quantity,
                                BigDecimal limitPrice, long filledQuantity, long leavesQuantity,
                                BigDecimal averagePrice, String status, List<ChildOrderResponse> childOrders) {
        static OrderResponse from(ParentOrder o) {
            return new OrderResponse(o.getId(), o.getClientOrderId(), o.getSymbol(), o.getSide(), o.getQuantity(),
                    o.getLimitPrice(), o.getFilledQuantity(), o.getLeavesQuantity(), o.getAveragePrice(),
                    o.getStatus().name(), o.getChildren().stream().map(ChildOrderResponse::from).toList());
        }
    }
}
