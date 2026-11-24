package com.dke.data_engineering.data_quality_and_validation.api;

import com.dke.data_engineering.data_quality_and_validation.domain.RuleViolation;
import com.dke.data_engineering.data_quality_and_validation.service.ValidationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runs")
public class ValidationController {

    private final ValidationService service;

    public ValidationController(ValidationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse validate(@Valid @RequestBody ValidateRequest request) {
        return RunResponse.from(service.validate(request.businessDate()));
    }

    @PostMapping("/{id}/release")
    public RunResponse release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
        return RunResponse.from(service.release(id, request.approvedBy(), request.reason()));
    }

    @GetMapping
    public List<RunResponse> list() {
        return service.list().stream().map(RunResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RunResponse get(@PathVariable Long id) {
        return RunResponse.from(service.get(id));
    }

    @GetMapping("/{id}/violations")
    public List<RuleViolation> violations(@PathVariable Long id) {
        return service.violations(id);
    }
}
