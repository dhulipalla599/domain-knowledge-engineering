package com.dke.data_engineering.data_quality_and_validation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseRequest(@NotBlank String approvedBy, @NotBlank @Size(min = 10, max = 500) String reason) { }
