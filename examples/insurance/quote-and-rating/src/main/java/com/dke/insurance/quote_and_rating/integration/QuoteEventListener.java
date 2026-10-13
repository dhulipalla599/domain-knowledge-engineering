package com.dke.insurance.quote_and_rating.integration;

import com.dke.insurance.quote_and_rating.domain.QuoteEvents.QuoteBound;
import com.dke.insurance.quote_and_rating.domain.QuoteEvents.QuoteRated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers: analytics on rated quotes, policy issuance on bound quotes. */
@Component
public class QuoteEventListener {

    private static final Logger log = LoggerFactory.getLogger(QuoteEventListener.class);

    private final PolicyIssuanceGateway policies;

    public QuoteEventListener(PolicyIssuanceGateway policies) {
        this.policies = policies;
    }

    @EventListener
    public void onQuoteRated(QuoteRated event) {
        log.info("[insurance.quote.rated] quote={} premium={}", event.quoteId(), event.premium());
    }

    @EventListener
    public void onQuoteBound(QuoteBound event) {
        String policyNumber = policies.issuePolicy(event.quoteId(), event.driverName());
        log.info("[insurance.quote.bound] quote={} -> policy {}", event.quoteId(), policyNumber);
    }
}
