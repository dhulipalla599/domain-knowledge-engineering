package com.dke.healthcare.electronic_health_records_ehr.api;

import jakarta.validation.constraints.NotBlank;

public record NoteRequest(@NotBlank String authorId, String text) { }
