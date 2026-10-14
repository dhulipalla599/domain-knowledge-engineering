package com.dke.insurance.quote_and_rating.service;

import com.dke.insurance.quote_and_rating.api.CreateQuoteRequest;
import com.dke.insurance.quote_and_rating.domain.BaseRate;
import com.dke.insurance.quote_and_rating.domain.Quote;
import com.dke.insurance.quote_and_rating.domain.QuoteEvents.QuoteBound;
import com.dke.insurance.quote_and_rating.domain.QuoteEvents.QuoteRated;
import com.dke.insurance.quote_and_rating.domain.QuoteRuleException;
import com.dke.insurance.quote_and_rating.integration.CreditScoreProvider;
import com.dke.insurance.quote_and_rating.repository.BaseRateRepository;
import com.dke.insurance.quote_and_rating.repository.QuoteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService {

    static final int MINIMUM_AGE = 18;
    static final int MAX_PRIOR_CLAIMS = 3;
    static final Duration QUOTE_VALIDITY = Duration.ofDays(30);
    /** States that restrict credit-based insurance scores in rating (illustrative: confirm per state). */
    static final Set<String> SCORE_RESTRICTED_STATES = Set.of("CA");

    private final QuoteRepository quotes;
    private final BaseRateRepository baseRates;
    private final RatingEngine ratingEngine;
    private final CreditScoreProvider scores;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public QuoteService(QuoteRepository quotes, BaseRateRepository baseRates, RatingEngine ratingEngine,
                        CreditScoreProvider scores, ApplicationEventPublisher events, Clock clock) {
        this.quotes = quotes;
        this.baseRates = baseRates;
        this.ratingEngine = ratingEngine;
        this.scores = scores;
        this.events = events;
        this.clock = clock;
    }

    /** Rule 1: ineligible risks are declined before any premium is computed or stored. */
    @Transactional
    public Quote createQuote(CreateQuoteRequest request) {
        if (request.driverAge() < MINIMUM_AGE) {
            throw new QuoteRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Driver must be at least " + MINIMUM_AGE + " years old");
        }
        if (request.priorClaims() > MAX_PRIOR_CLAIMS) {
            throw new QuoteRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "More than " + MAX_PRIOR_CLAIMS + " claims in the lookback period: refer to underwriter");
        }
        BaseRate base = baseRates.findById(request.state()).orElseThrow(() ->
                new QuoteRuleException(HttpStatus.UNPROCESSABLE_ENTITY, "State not served: " + request.state()));

        OptionalInt score = SCORE_RESTRICTED_STATES.contains(request.state())
                ? OptionalInt.empty()
                : OptionalInt.of(scores.scoreFor(request.driverName()));

        Quote quote = new Quote(request.driverName(), request.driverAge(), request.state(),
                request.vehicleType(), request.priorClaims());
        var premium = ratingEngine.rate(base.getAnnualAmount(), request.driverAge(), request.vehicleType(),
                request.priorClaims(), score);
        quote.markRated(premium, clock.instant().plus(QUOTE_VALIDITY));
        quotes.save(quote);
        events.publishEvent(new QuoteRated(quote.getId(), premium, quote.getValidUntil()));
        return quote;
    }

    /** Rule 2: a price is only honoured until validUntil; an expired quote must be re-rated, never bound. */
    @Transactional(noRollbackFor = QuoteRuleException.class)
    public Quote bind(UUID id) {
        Quote quote = find(id);
        Instant now = clock.instant();
        if (quote.isExpiredAt(now)) {
            quote.expire();
            quotes.save(quote);
            throw new QuoteRuleException(HttpStatus.CONFLICT, "Quote " + id + " expired; request a new quote");
        }
        quote.bind(now);
        quotes.save(quote);
        events.publishEvent(new QuoteBound(quote.getId(), quote.getDriverName(), quote.getPremium(), now));
        return quote;
    }

    @Transactional(readOnly = true)
    public Quote find(UUID id) {
        return quotes.findById(id).orElseThrow(() ->
                new QuoteRuleException(HttpStatus.NOT_FOUND, "Quote not found: " + id));
    }
}
