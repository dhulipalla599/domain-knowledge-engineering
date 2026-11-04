package com.dke.telecom.subscriber_onboarding.service;

import com.dke.telecom.subscriber_onboarding.api.OnboardingException;
import com.dke.telecom.subscriber_onboarding.api.OnboardingRequest;
import com.dke.telecom.subscriber_onboarding.domain.*;
import com.dke.telecom.subscriber_onboarding.integration.IdentityVerifier;
import com.dke.telecom.subscriber_onboarding.repository.PhoneNumberRepository;
import com.dke.telecom.subscriber_onboarding.repository.SimCardRepository;
import com.dke.telecom.subscriber_onboarding.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

    static final int MIN_AGE = 18;
    static final int MAX_ACTIVE_LINES_PER_ID = 5;   // the limit differs by country and operator

    private final SubscriptionRepository subscriptions;
    private final SimCardRepository simCards;
    private final PhoneNumberRepository numbers;
    private final IdentityVerifier verifier;
    private final ApplicationEventPublisher events;

    public OnboardingService(SubscriptionRepository subscriptions, SimCardRepository simCards,
                             PhoneNumberRepository numbers, IdentityVerifier verifier,
                             ApplicationEventPublisher events) {
        this.subscriptions = subscriptions;
        this.simCards = simCards;
        this.numbers = numbers;
        this.verifier = verifier;
        this.events = events;
    }

    @Transactional
    public Subscription onboard(String idempotencyKey, OnboardingRequest request) {
        var replay = subscriptions.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }
        if (request.dateOfBirth().plusYears(MIN_AGE).isAfter(LocalDate.now())) {
            throw new OnboardingException(HttpStatus.UNPROCESSABLE_ENTITY, "Applicant too young",
                    "Applicants must be at least " + MIN_AGE + " years old");
        }
        long activeLines = subscriptions.countByIdDocumentNumberAndStatus(
                request.idDocumentNumber(), SubscriptionStatus.ACTIVE);
        if (activeLines >= MAX_ACTIVE_LINES_PER_ID) {
            throw new OnboardingException(HttpStatus.UNPROCESSABLE_ENTITY, "Line limit reached",
                    "This ID already has " + activeLines + " active lines");
        }
        SimCard sim = simCards.findById(request.iccid()).orElseThrow(() -> new OnboardingException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Unknown SIM", "No SIM with ICCID " + request.iccid()));
        if (!sim.isAvailable()) {
            throw new OnboardingException(HttpStatus.CONFLICT, "SIM already assigned",
                    "SIM " + sim.getIccid() + " is already in use");
        }

        Subscription subscription = subscriptions.save(Subscription.submit(idempotencyKey, request.fullName(),
                request.dateOfBirth(), request.idDocumentNumber(), request.plan(), request.iccid()));
        events.publishEvent(new OnboardingEvent.Submitted(subscription.getId(), sim.getIccid()));

        var check = verifier.verify(request.fullName(), request.idDocumentNumber(), request.dateOfBirth());
        if (!check.verified()) {
            subscription.reject(check.reason());
            events.publishEvent(new OnboardingEvent.Rejected(subscription.getId(), check.reason()));
            return subscription;
        }
        subscription.markVerified();

        PhoneNumber number = numbers.findFirstByStatusOrderByMsisdn(PhoneNumber.Status.AVAILABLE)
                .orElseThrow(() -> new OnboardingException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No numbers available", "The number pool is exhausted; try again later"));
        number.markInUse();
        sim.assign(number.getMsisdn());
        subscription.activate(number.getMsisdn());
        events.publishEvent(new OnboardingEvent.Activated(subscription.getId(), number.getMsisdn(),
                subscription.getPlan()));
        return subscription;
    }

    @Transactional(readOnly = true)
    public Subscription get(UUID id) {
        return subscriptions.findById(id).orElseThrow(() -> new OnboardingException(
                HttpStatus.NOT_FOUND, "Subscription not found", "No subscription " + id));
    }

    @Transactional
    public Subscription cancel(UUID id) {
        Subscription subscription = get(id);
        subscription.cancel();
        return subscription;
    }
}
