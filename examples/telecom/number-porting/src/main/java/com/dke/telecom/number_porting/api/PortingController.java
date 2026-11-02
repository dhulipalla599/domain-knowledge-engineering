package com.dke.telecom.number_porting.api;

import com.dke.telecom.number_porting.service.PortingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/port-requests")
public class PortingController {

    private final PortingService service;

    public PortingController(PortingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PortRequestResponse> submit(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                      @Valid @RequestBody SubmitPortRequest body) {
        var result = service.submit(body.msisdn(), body.subscriberName(), body.donorOperator(),
                body.requestedPortDate(), idempotencyKey);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(PortRequestResponse.from(result.request()));
    }

    @GetMapping
    public List<PortRequestResponse> list() {
        return service.list().stream().map(PortRequestResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PortRequestResponse get(@PathVariable Long id) {
        return PortRequestResponse.from(service.get(id));
    }

    @PostMapping("/{id}/complete")
    public PortRequestResponse complete(@PathVariable Long id) {
        return PortRequestResponse.from(service.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public PortRequestResponse cancel(@PathVariable Long id) {
        return PortRequestResponse.from(service.cancel(id));
    }
}
