package com.dke.telecom.subscriber_onboarding.api;

import com.dke.telecom.subscriber_onboarding.domain.Subscription;
import com.dke.telecom.subscriber_onboarding.domain.SubscriptionStatus;
import com.dke.telecom.subscriber_onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final OnboardingService onboarding;

    public SubscriptionController(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @PostMapping
    ResponseEntity<SubscriptionResponse> onboard(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                 @Valid @RequestBody OnboardingRequest request) {
        Subscription subscription = onboarding.onboard(idempotencyKey, request);
        if (subscription.getStatus() == SubscriptionStatus.REJECTED) {
            throw new OnboardingException(HttpStatus.UNPROCESSABLE_ENTITY, "Identity verification failed",
                    subscription.getRejectionReason());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(subscription));
    }

    @GetMapping("/{id}")
    SubscriptionResponse get(@PathVariable UUID id) {
        return SubscriptionResponse.from(onboarding.get(id));
    }

    @PostMapping("/{id}/cancel")
    SubscriptionResponse cancel(@PathVariable UUID id) {
        return SubscriptionResponse.from(onboarding.cancel(id));
    }
}
