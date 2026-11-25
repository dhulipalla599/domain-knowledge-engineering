package com.dke.data_engineering.data_quality_and_validation.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ValidateRequest(@NotNull LocalDate businessDate) { }
