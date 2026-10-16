package com.dke.logistics.shipment_booking.api;

import com.dke.logistics.shipment_booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                @Valid @RequestBody BookingRequest request) {
        return BookingResponse.from(service.book(idempotencyKey, request.customerId(), request.quoteId(),
                request.weightKg(), request.unNumber()));
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable UUID id) {
        return BookingResponse.from(service.get(id));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id) {
        return BookingResponse.from(service.cancel(id));
    }
}
