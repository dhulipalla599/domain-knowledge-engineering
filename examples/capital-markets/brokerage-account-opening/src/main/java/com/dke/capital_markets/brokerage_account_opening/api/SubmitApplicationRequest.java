package com.dke.capital_markets.brokerage_account_opening.api;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitApplicationRequest(
        @NotBlank String applicantName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits") String taxIdLast4,
        @NotNull AccountType accountType,
        @NotNull @PositiveOrZero BigDecimal liquidNetWorth) {
}
