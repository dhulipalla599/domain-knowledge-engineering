package com.dke.airline.passenger_name_record_pnr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreatePnrRequest(@NotBlank @Email String contactEmail,
                               @NotEmpty List<@Valid PassengerDto> passengers,
                               @NotEmpty List<@Valid SegmentDto> segments) {
}
