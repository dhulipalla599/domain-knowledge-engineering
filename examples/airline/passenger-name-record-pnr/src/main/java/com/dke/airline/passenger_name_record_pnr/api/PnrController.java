package com.dke.airline.passenger_name_record_pnr.api;

import com.dke.airline.passenger_name_record_pnr.domain.FlightSegment;
import com.dke.airline.passenger_name_record_pnr.domain.PassengerName;
import com.dke.airline.passenger_name_record_pnr.service.PnrService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pnrs")
public class PnrController {

    private final PnrService service;

    public PnrController(PnrService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PnrResponse> create(@Valid @RequestBody CreatePnrRequest request) {
        var pnr = service.create(request.contactEmail(),
                request.passengers().stream().map(p -> new PassengerName(p.firstName(), p.lastName())).toList(),
                request.segments().stream().map(s -> new FlightSegment(s.flightNumber(), s.departureDate(), s.origin(), s.destination())).toList());
        return ResponseEntity.created(URI.create("/api/pnrs/" + pnr.getRecordLocator())).body(PnrResponse.from(pnr));
    }

    @GetMapping("/{recordLocator}")
    public PnrResponse get(@PathVariable String recordLocator) {
        return PnrResponse.from(service.get(recordLocator));
    }

    @PostMapping("/{recordLocator}/ticket")
    public PnrResponse ticket(@PathVariable String recordLocator) {
        return PnrResponse.from(service.ticket(recordLocator));
    }

    @PostMapping("/{recordLocator}/cancel")
    public PnrResponse cancel(@PathVariable String recordLocator, @RequestBody(required = false) CancelRequest request) {
        return PnrResponse.from(service.cancel(recordLocator, request == null ? null : request.reason()));
    }
}
