package com.dke.logistics.warehouse_management.api;

import jakarta.validation.constraints.NotBlank;

public record PutAwayRequest(@NotBlank String binCode) {
}
