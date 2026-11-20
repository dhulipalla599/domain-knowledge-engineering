package com.dke.banking.card_authorization_and_settlement.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** What the card network forwards to the issuer for an authorization (simplified ISO 8583 0100). */
public record AuthorizationRequest(
        @NotBlank String networkReference,
        @NotBlank String cardToken,
        @NotBlank String merchantName,
        @NotNull @Pattern(regexp = "\\d{4}") String mcc,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {
}
