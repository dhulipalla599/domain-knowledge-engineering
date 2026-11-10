package com.dke.insurance.policy_administration.service;

import com.dke.insurance.policy_administration.domain.*;
import com.dke.insurance.policy_administration.domain.PolicyEvent.*;
import com.dke.insurance.policy_administration.integration.PaymentGateway;
import com.dke.insurance.policy_administration.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules of policy administration: issue, endorse, cancel, expire. */
@Service
public class PolicyService {

    private final PolicyRepository repository;
    private final PaymentGateway payments;
    private final ApplicationEventPublisher events;

    public PolicyService(PolicyRepository repository, PaymentGateway payments, ApplicationEventPublisher events) {
        this.repository = repository;
        this.payments = payments;
        this.events = events;
    }

    /** Rule: one policy per bound quote (idempotent issuance); the first premium must be collected. */
    @Transactional
    public Policy issue(String quoteId, String holderName, BigDecimal sumInsured,
                        BigDecimal rate, LocalDate inception) {
        if (repository.existsByQuoteId(quoteId)) {
            throw new RuleViolationException(HttpStatus.CONFLICT, "Quote " + quoteId + " already has a policy");
        }
        var policy = repository.save(Policy.issue(quoteId, holderName, sumInsured, rate, inception));
        policy.assignNumber("HM-%d-%06d".formatted(inception.getYear(), policy.getId()));
        var result = payments.charge(policy.getPolicyNumber(), policy.getAnnualPremium());
        if (!result.approved()) {
            throw new RuleViolationException(HttpStatus.PAYMENT_REQUIRED, "First premium declined: " + result.message());
        }
        events.publishEvent(new PolicyIssued(policy.getId(), policy.getPolicyNumber(), policy.getAnnualPremium()));
        return policy;
    }

    /** Rule: an endorsement must take effect inside the term and is charged or refunded pro rata. */
    @Transactional
    public Policy endorse(Long id, BigDecimal newSumInsured, LocalDate effective, String reason) {
        var policy = find(id);
        policy.requireActive("endorse");
        if (effective.isBefore(policy.getInception()) || !effective.isBefore(policy.getExpiry())) {
            throw new RuleViolationException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Effective date " + effective + " is outside the policy term "
                            + policy.getInception() + " to " + policy.getExpiry());
        }
        BigDecimal newAnnual = Policy.premiumFor(newSumInsured, policy.getRate());
        BigDecimal delta = ProRata.remainingShare(newAnnual.subtract(policy.getAnnualPremium()),
                policy.getInception(), policy.getExpiry(), effective);
        if (delta.signum() > 0) {
            var result = payments.charge(policy.getPolicyNumber(), delta);
            if (!result.approved()) {
                throw new RuleViolationException(HttpStatus.PAYMENT_REQUIRED,
                        "Additional premium declined: " + result.message());
            }
        } else if (delta.signum() < 0) {
            payments.refund(policy.getPolicyNumber(), delta.negate());
        }
        policy.endorse(newSumInsured, newAnnual, effective, delta, reason);
        events.publishEvent(new PolicyEndorsed(id, effective, delta));
        return policy;
    }

    /** Rule: cancellation refunds the unexpired share of the premium; a policy can only be cancelled once. */
    @Transactional
    public Policy cancel(Long id, LocalDate effective) {
        var policy = find(id);
        policy.requireActive("cancel");
        if (effective.isBefore(policy.getInception()) || effective.isAfter(policy.getExpiry())) {
            throw new RuleViolationException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cancellation date " + effective + " is outside the policy term");
        }
        BigDecimal refund = ProRata.remainingShare(policy.getAnnualPremium(),
                policy.getInception(), policy.getExpiry(), effective);
        policy.cancel(effective, refund);
        payments.refund(policy.getPolicyNumber(), refund);
        events.publishEvent(new PolicyCancelled(id, effective, refund));
        return policy;
    }

    /** Stand-in for the nightly expiry batch. */
    @Transactional
    public Policy expire(Long id, LocalDate asOf) {
        var policy = find(id);
        if (asOf.isBefore(policy.getExpiry())) {
            throw new RuleViolationException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Policy runs until " + policy.getExpiry() + "; it cannot expire on " + asOf);
        }
        policy.expire();
        events.publishEvent(new PolicyExpired(id));
        return policy;
    }

    @Transactional(readOnly = true)
    public Policy get(Long id) {
        return find(id);
    }

    @Transactional(readOnly = true)
    public List<Policy> list() {
        return repository.findAll();
    }

    private Policy find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuleViolationException(HttpStatus.NOT_FOUND, "Policy " + id + " not found"));
    }
}
