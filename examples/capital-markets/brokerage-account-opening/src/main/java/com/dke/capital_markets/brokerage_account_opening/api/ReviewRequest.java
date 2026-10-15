package com.dke.capital_markets.brokerage_account_opening.api;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(boolean approved, @NotBlank String note) {
}
