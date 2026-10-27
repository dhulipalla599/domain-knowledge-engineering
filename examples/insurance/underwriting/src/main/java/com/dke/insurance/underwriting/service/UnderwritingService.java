package com.dke.insurance.underwriting.service;

import com.dke.insurance.underwriting.domain.ApplicationStatus;
import com.dke.insurance.underwriting.domain.PolicyApplication;
import com.dke.insurance.underwriting.domain.UnderwritingEvent.ApplicationSubmitted;
import com.dke.insurance.underwriting.domain.UnderwritingEvent.DecisionMade;
import com.dke.insurance.underwriting.integration.AuthorityDirectory;
import com.dke.insurance.underwriting.integration.ClaimsHistoryClient;
import com.dke.insurance.underwriting.repository.PolicyApplicationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnderwritingService {

    public record Submission(PolicyApplication application, boolean created) {
    }

    private final PolicyApplicationRepository repository;
    private final RiskScorer scorer;
    private final ClaimsHistoryClient claimsHistory;
    private final AuthorityDirectory authorities;
    private final ApplicationEventPublisher events;

    public UnderwritingService(PolicyApplicationRepository repository, RiskScorer scorer,
                               ClaimsHistoryClient claimsHistory, AuthorityDirectory authorities,
                               ApplicationEventPublisher events) {
        this.repository = repository;
        this.scorer = scorer;
        this.claimsHistory = claimsHistory;
        this.authorities = authorities;
        this.events = events;
    }

    /** Scores a new application. Replaying the same idempotency key returns the original, never a duplicate. */
    @Transactional
    public Submission submit(String idempotencyKey, String applicantName, BigDecimal sumInsured,
                             int yearBuilt, boolean floodZone) {
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new Submission(existing.get(), false);
        }
        int claims = claimsHistory.claimsInLastFiveYears(applicantName);
        var assessment = scorer.assess(sumInsured, yearBuilt, floodZone, claims);

        var application = new PolicyApplication(idempotencyKey, applicantName, sumInsured,
                yearBuilt, floodZone, claims, assessment.score());
        application.moveTo(assessment.outcome(), assessment.reason(), "auto-rules");
        repository.save(application);

        events.publishEvent(new ApplicationSubmitted(application.getId(), assessment.score()));
        if (assessment.outcome() != ApplicationStatus.REFERRED) {
            events.publishEvent(new DecisionMade(application.getId(), assessment.outcome(),
                    "auto-rules", assessment.reason()));
        }
        return new Submission(application, true);
    }

    /** A named underwriter decides a referred case; approvals are capped by their signing authority. */
    @Transactional
    public PolicyApplication decide(Long id, String underwriterId, boolean approve, String reason) {
        var application = repository.findById(id)
                .orElseThrow(() -> new RuleViolationException(HttpStatus.NOT_FOUND, "Application " + id + " not found"));
        BigDecimal limit = authorities.approvalLimit(underwriterId)
                .orElseThrow(() -> new RuleViolationException(HttpStatus.FORBIDDEN,
                        "Unknown underwriter " + underwriterId));
        if (approve && application.getSumInsured().compareTo(limit) > 0) {
            throw new RuleViolationException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Sum insured " + application.getSumInsured() + " exceeds authority limit " + limit
                            + " of " + underwriterId + "; escalate to a senior underwriter");
        }
        var next = approve ? ApplicationStatus.APPROVED : ApplicationStatus.DECLINED;
        application.moveTo(next, reason, underwriterId);
        repository.save(application);
        events.publishEvent(new DecisionMade(id, next, underwriterId, reason));
        return application;
    }

    @Transactional(readOnly = true)
    public PolicyApplication get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuleViolationException(HttpStatus.NOT_FOUND, "Application " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<PolicyApplication> referralQueue() {
        return repository.findByStatusOrderByCreatedAtAsc(ApplicationStatus.REFERRED);
    }
}
