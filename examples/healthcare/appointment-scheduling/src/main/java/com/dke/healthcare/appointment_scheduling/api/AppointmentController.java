package com.dke.healthcare.appointment_scheduling.api;

import com.dke.healthcare.appointment_scheduling.service.AppointmentService;
import com.dke.healthcare.appointment_scheduling.service.AppointmentService.BookingOutcome;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(
            @Valid @RequestBody BookAppointmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        BookingOutcome outcome = service.book(request.patientId(), request.providerId(),
                request.startTime(), request.reason(), idempotencyKey);
        var body = AppointmentResponse.from(outcome.appointment());
        if (!outcome.created()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.created(URI.create("/api/appointments/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable UUID id) {
        return AppointmentResponse.from(service.get(id));
    }

    @GetMapping
    public List<AppointmentResponse> forProvider(@RequestParam String providerId) {
        return service.forProvider(providerId).stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id) {
        return AppointmentResponse.from(service.cancel(id));
    }

    @PostMapping("/{id}/check-in")
    public AppointmentResponse checkIn(@PathVariable UUID id) {
        return AppointmentResponse.from(service.checkIn(id));
    }
}
