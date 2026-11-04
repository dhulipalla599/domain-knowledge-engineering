package com.dke.banking.payments_processing_ach_wire_rtp.api;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.Rail;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String fromAccount,
        @NotNull Rail rail,
        @NotBlank String beneficiaryName,
        @NotNull @Pattern(regexp = "\\d{9}", message = "must be a 9-digit routing number") String beneficiaryRouting,
        @NotBlank String beneficiaryAccount,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount) {
}
