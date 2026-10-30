package com.dke.logistics.warehouse_management.api;

import jakarta.validation.constraints.Positive;

public record ReceiveRequest(@Positive int countedQty) {
}
