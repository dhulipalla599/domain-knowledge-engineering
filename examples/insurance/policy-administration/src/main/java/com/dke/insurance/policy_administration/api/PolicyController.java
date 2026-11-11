package com.dke.insurance.policy_administration.api;

import com.dke.insurance.policy_administration.service.PolicyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> issue(@Valid @RequestBody IssuePolicyRequest request) {
        var policy = service.issue(request.quoteId(), request.holderName(), request.sumInsured(),
                request.rate(), request.inception());
        return ResponseEntity.created(URI.create("/api/policies/" + policy.getId()))
                .body(PolicyResponse.of(policy));
    }

    @GetMapping
    public List<PolicyResponse> list() {
        return service.list().stream().map(PolicyResponse::of).toList();
    }

    @GetMapping("/{id}")
    public PolicyResponse get(@PathVariable Long id) {
        return PolicyResponse.of(service.get(id));
    }

    @PostMapping("/{id}/endorsements")
    public PolicyResponse endorse(@PathVariable Long id, @Valid @RequestBody EndorsementRequest request) {
        return PolicyResponse.of(service.endorse(id, request.newSumInsured(), request.effectiveDate(), request.reason()));
    }

    @PostMapping("/{id}/cancellation")
    public PolicyResponse cancel(@PathVariable Long id, @Valid @RequestBody DateRequest request) {
        return PolicyResponse.of(service.cancel(id, request.effectiveDate()));
    }

    @PostMapping("/{id}/expiry")
    public PolicyResponse expire(@PathVariable Long id, @Valid @RequestBody DateRequest request) {
        return PolicyResponse.of(service.expire(id, request.effectiveDate()));
    }
}
