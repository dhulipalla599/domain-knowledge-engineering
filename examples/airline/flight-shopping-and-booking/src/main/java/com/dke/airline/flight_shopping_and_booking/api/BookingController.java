package com.dke.airline.flight_shopping_and_booking.api;

import com.dke.airline.flight_shopping_and_booking.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @GetMapping("/offers")
    public List<OfferResponse> search(@RequestParam String origin, @RequestParam String destination,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.search(origin, destination, date).stream().map(OfferResponse::from).toList();
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> hold(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                @Valid @RequestBody HoldRequest request) {
        var result = service.hold(idempotencyKey, request.offerId(), request.passengerName(),
                request.passengerEmail(), request.expectedPrice());
        // 201 for a new hold, 200 when the same Idempotency-Key is replayed.
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(BookingResponse.from(result.booking()));
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse get(@PathVariable UUID id) {
        return BookingResponse.from(service.get(id));
    }

    @PostMapping("/bookings/{id}/confirm")
    public BookingResponse confirm(@PathVariable UUID id, @Valid @RequestBody ConfirmRequest request) {
        return BookingResponse.from(service.confirm(id, request.paymentToken()));
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelRequest request) {
        return BookingResponse.from(service.cancel(id, request.reason()));
    }

    /** Operational endpoint; in production a scheduled job calls the service directly. */
    @PostMapping("/admin/expire-holds")
    public Map<String, Integer> expireHolds() {
        return Map.of("expired", service.expireStaleHolds());
    }
}
