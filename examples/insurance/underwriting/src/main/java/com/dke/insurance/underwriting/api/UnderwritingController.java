package com.dke.insurance.underwriting.api;

import com.dke.insurance.underwriting.domain.UnderwritingEvent;
import com.dke.insurance.underwriting.integration.UnderwritingEventListener;
import com.dke.insurance.underwriting.service.UnderwritingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class UnderwritingController {

    private final UnderwritingService service;
    private final UnderwritingEventListener eventLog;

    public UnderwritingController(UnderwritingService service, UnderwritingEventListener eventLog) {
        this.service = service;
        this.eventLog = eventLog;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> submit(@RequestHeader("Idempotency-Key") String key,
                                                      @Valid @RequestBody SubmitApplicationRequest request) {
        var result = service.submit(key, request.applicantName(), request.sumInsured(),
                request.yearBuilt(), request.floodZone());
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApplicationResponse.from(result.application()));
    }

    @PostMapping("/{id}/decision")
    public ApplicationResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApplicationResponse.from(
                service.decide(id, request.underwriterId(), request.approve(), request.reason()));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return ApplicationResponse.from(service.get(id));
    }

    @GetMapping("/referrals")
    public List<ApplicationResponse> referrals() {
        return service.referralQueue().stream().map(ApplicationResponse::from).toList();
    }

    @GetMapping("/events")
    public List<UnderwritingEvent> events() {
        return eventLog.published();
    }
}
