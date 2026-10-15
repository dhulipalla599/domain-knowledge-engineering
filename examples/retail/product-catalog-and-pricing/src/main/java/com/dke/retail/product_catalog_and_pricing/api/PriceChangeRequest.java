package com.dke.retail.product_catalog_and_pricing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PriceChangeRequest(@NotBlank String sku, @NotNull BigDecimal newPrice, @NotBlank String reason) {
}
