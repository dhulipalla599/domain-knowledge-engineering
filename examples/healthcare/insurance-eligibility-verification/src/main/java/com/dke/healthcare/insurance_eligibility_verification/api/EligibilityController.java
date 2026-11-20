package com.dke.healthcare.insurance_eligibility_verification.api;

import com.dke.healthcare.insurance_eligibility_verification.service.EligibilityService;
import com.dke.healthcare.insurance_eligibility_verification.service.EligibilityService.VerificationResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eligibility-checks")
public class EligibilityController {

    private final EligibilityService service;

    public EligibilityController(EligibilityService service) {
        this.service = service;
    }

    /** 201 with a new check, or 200 when a fresh answer for the same question was reused. */
    @PostMapping
    public ResponseEntity<EligibilityCheckResponse> verify(@Valid @RequestBody EligibilityCheckRequest request) {
        VerificationResult result = service.verify(request.coverageId(), request.serviceDate(), request.serviceTypeCode());
        EligibilityCheckResponse body = EligibilityCheckResponse.of(result.check(), result.reused());
        return result.reused()
                ? ResponseEntity.ok(body)
                : ResponseEntity.created(URI.create("/api/eligibility-checks/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public EligibilityCheckResponse get(@PathVariable String id) {
        return EligibilityCheckResponse.of(service.get(id), false);
    }

    @GetMapping
    public List<EligibilityCheckResponse> history(@RequestParam String coverageId) {
        return service.history(coverageId).stream().map(c -> EligibilityCheckResponse.of(c, false)).toList();
    }

    @PostMapping("/{id}/retry")
    public EligibilityCheckResponse retry(@PathVariable String id) {
        return EligibilityCheckResponse.of(service.retry(id), false);
    }
}
