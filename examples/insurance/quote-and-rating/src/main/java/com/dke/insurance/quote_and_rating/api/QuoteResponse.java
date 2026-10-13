package com.dke.insurance.quote_and_rating.api;

import com.dke.insurance.quote_and_rating.domain.Quote;
import com.dke.insurance.quote_and_rating.domain.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuoteResponse(UUID id, String driverName, String state, QuoteStatus status,
                            BigDecimal premium, Instant validUntil, Instant boundAt) {

    public static QuoteResponse from(Quote q) {
        return new QuoteResponse(q.getId(), q.getDriverName(), q.getState(), q.getStatus(),
                q.getPremium(), q.getValidUntil(), q.getBoundAt());
    }
}
