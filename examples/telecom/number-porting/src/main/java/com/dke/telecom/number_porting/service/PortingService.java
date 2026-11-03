package com.dke.telecom.number_porting.service;

import com.dke.telecom.number_porting.domain.*;
import com.dke.telecom.number_porting.domain.PortEvents.*;
import com.dke.telecom.number_porting.integration.DonorGateway;
import com.dke.telecom.number_porting.repository.PortRequestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortingService {

    public static final String RECIPIENT = "Northwind Mobile";
    private static final List<PortStatus> IN_FLIGHT =
            List.of(PortStatus.SUBMITTED, PortStatus.VALIDATED, PortStatus.SCHEDULED);

    public record Submission(PortRequest request, boolean created) {}

    private final PortRequestRepository repository;
    private final DonorGateway donor;
    private final PortDatePolicy datePolicy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PortingService(PortRequestRepository repository, DonorGateway donor, PortDatePolicy datePolicy,
                          ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.donor = donor;
        this.datePolicy = datePolicy;
        this.events = events;
        this.clock = clock;
    }

    /** Core rules: idempotent submit, one in-flight port per number, valid port date, donor must accept. */
    @Transactional
    public Submission submit(String msisdn, String subscriberName, String donorOperator,
                             LocalDate requestedDate, String idempotencyKey) {
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new Submission(existing.get(), false);
        }
        if (repository.existsByMsisdnAndStatusIn(msisdn, IN_FLIGHT)) {
            throw new PortRuleViolation("DUPLICATE_PORT", "A port request is already in flight for " + msisdn);
        }
        String dateProblem = datePolicy.violation(requestedDate, LocalDate.now(clock));
        if (dateProblem != null) {
            throw new PortRuleViolation("INVALID_PORT_DATE", dateProblem);
        }

        PortRequest request = repository.save(
                new PortRequest(msisdn, subscriberName, donorOperator, RECIPIENT, requestedDate, idempotencyKey));
        events.publishEvent(new PortRequestSubmitted(request.getId(), msisdn, requestedDate));

        var decision = donor.validate(msisdn, subscriberName, donorOperator);
        if (!decision.accepted()) {
            request.reject(decision.reason());
            events.publishEvent(new PortRequestRejected(request.getId(), msisdn, decision.reason()));
            return new Submission(request, true);
        }
        request.transitionTo(PortStatus.VALIDATED);
        events.publishEvent(new PortRequestValidated(request.getId(), msisdn));
        request.transitionTo(PortStatus.SCHEDULED);
        events.publishEvent(new PortRequestScheduled(request.getId(), msisdn, requestedDate));
        return new Submission(request, true);
    }

    /** Cut-over: only on or after the agreed port date. */
    @Transactional
    public PortRequest complete(Long id) {
        PortRequest request = get(id);
        if (LocalDate.now(clock).isBefore(request.getRequestedPortDate())) {
            throw new PortRuleViolation("TOO_EARLY", "Cut-over not allowed before " + request.getRequestedPortDate());
        }
        request.transitionTo(PortStatus.COMPLETED);
        events.publishEvent(new PortCompleted(id, request.getMsisdn(), request.getRecipientOperator()));
        return request;
    }

    @Transactional
    public PortRequest cancel(Long id) {
        PortRequest request = get(id);
        request.transitionTo(PortStatus.CANCELLED);
        events.publishEvent(new PortRequestCancelled(id, request.getMsisdn()));
        return request;
    }

    @Transactional(readOnly = true)
    public PortRequest get(Long id) {
        return repository.findById(id).orElseThrow(() -> new PortRequestNotFound(id));
    }

    @Transactional(readOnly = true)
    public List<PortRequest> list() {
        return repository.findAll();
    }
}
