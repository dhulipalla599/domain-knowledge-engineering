package com.dke.insurance.quote_and_rating.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Domain events. In production each becomes a message on the Kafka topic named in its comment. */
public final class QuoteEvents {

    private QuoteEvents() {
    }

    /** Topic: insurance.quote.rated */
    public record QuoteRated(UUID quoteId, BigDecimal premium, Instant validUntil) {
    }

    /** Topic: insurance.quote.bound */
    public record QuoteBound(UUID quoteId, String driverName, BigDecimal premium, Instant boundAt) {
    }
}
