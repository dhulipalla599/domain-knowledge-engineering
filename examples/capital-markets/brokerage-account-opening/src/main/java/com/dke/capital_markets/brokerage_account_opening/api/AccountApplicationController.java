package com.dke.capital_markets.brokerage_account_opening.api;

import com.dke.capital_markets.brokerage_account_opening.domain.ApplicationStatus;
import com.dke.capital_markets.brokerage_account_opening.service.AccountOpeningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/applications")
public class AccountApplicationController {

    private final AccountOpeningService service;

    public AccountApplicationController(AccountOpeningService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> submit(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                      @Valid @RequestBody SubmitApplicationRequest request) {
        var result = service.submit(idempotencyKey, request.applicantName(), request.dateOfBirth(),
                request.taxIdLast4(), request.accountType(), request.liquidNetWorth());
        // 201 for a new application, 200 when the same Idempotency-Key is replayed.
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApplicationResponse.from(result.application()));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable UUID id) {
        return ApplicationResponse.from(service.get(id));
    }

    @GetMapping
    public List<ApplicationResponse> list(@RequestParam ApplicationStatus status) {
        return service.listByStatus(status).stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/{id}/review")
    public ApplicationResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        return ApplicationResponse.from(service.review(id, request.approved(), request.note()));
    }

    @PostMapping("/{id}/activate")
    public ApplicationResponse activate(@PathVariable UUID id) {
        return ApplicationResponse.from(service.activate(id));
    }
}
