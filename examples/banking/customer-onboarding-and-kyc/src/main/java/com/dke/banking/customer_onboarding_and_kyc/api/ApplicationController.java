package com.dke.banking.customer_onboarding_and_kyc.api;

import com.dke.banking.customer_onboarding_and_kyc.api.ApplicationDtos.ApplicationResponse;
import com.dke.banking.customer_onboarding_and_kyc.api.ApplicationDtos.ReviewCaseResponse;
import com.dke.banking.customer_onboarding_and_kyc.api.ApplicationDtos.ReviewDecisionRequest;
import com.dke.banking.customer_onboarding_and_kyc.api.ApplicationDtos.SubmitApplicationRequest;
import com.dke.banking.customer_onboarding_and_kyc.domain.CustomerAccount;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingApplication;
import com.dke.banking.customer_onboarding_and_kyc.repository.CustomerAccountRepository;
import com.dke.banking.customer_onboarding_and_kyc.service.OnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
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
@RequestMapping("/api/v1")
public class ApplicationController {

    private final OnboardingService onboarding;
    private final CustomerAccountRepository accounts;

    public ApplicationController(OnboardingService onboarding, CustomerAccountRepository accounts) {
        this.onboarding = onboarding;
        this.accounts = accounts;
    }

    /** Applicant submits. Requires an Idempotency-Key header so retries are safe. */
    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApplicationResponse submit(@RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                      @Valid @RequestBody SubmitApplicationRequest request) {
        OnboardingApplication application = onboarding.submit(idempotencyKey, request.legalName().trim(),
                request.dateOfBirth(), request.nationality(), request.occupation().trim());
        return toResponse(application);
    }

    @GetMapping("/applications/{id}")
    public ApplicationResponse get(@PathVariable UUID id) {
        return toResponse(onboarding.get(id));
    }

    /** Analyst work queue: applications waiting for a human decision. */
    @GetMapping("/review-cases")
    public List<ReviewCaseResponse> reviewQueue() {
        return onboarding.reviewQueue().stream().map(ReviewCaseResponse::from).toList();
    }

    @PostMapping("/review-cases/{id}/decision")
    public ReviewCaseResponse decide(@PathVariable UUID id, @Valid @RequestBody ReviewDecisionRequest request) {
        return ReviewCaseResponse.from(onboarding.resolveReview(id, request.cleared(), request.reason()));
    }

    private ApplicationResponse toResponse(OnboardingApplication a) {
        String accountNumber = accounts.findByApplicationId(a.getId())
                .map(CustomerAccount::getAccountNumber).orElse(null);
        String nextStep = switch (a.getStatus()) {
            case APPROVED -> "COMPLETE_WELCOME_STEPS";
            case REJECTED -> "NONE";
            default -> "WAIT_FOR_DECISION";
        };
        return new ApplicationResponse(a.getId(), a.getStatus().customerFacing(), nextStep, accountNumber);
    }
}
