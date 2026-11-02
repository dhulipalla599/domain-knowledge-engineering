package com.dke.telecom.number_porting.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SubmitPortRequest(
        @NotBlank @Pattern(regexp = "\\+[1-9]\\d{7,14}", message = "must be E.164, e.g. +15550101001") String msisdn,
        @NotBlank String subscriberName,
        @NotBlank String donorOperator,
        @NotNull LocalDate requestedPortDate) {}
