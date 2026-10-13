package com.dke.insurance.quote_and_rating.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.insurance.quote_and_rating.domain.Quote;
import com.dke.insurance.quote_and_rating.domain.QuoteRuleException;
import com.dke.insurance.quote_and_rating.domain.QuoteStatus;
import com.dke.insurance.quote_and_rating.domain.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class RatingEngineTest {

    private final RatingEngine engine = new RatingEngine();

    @Test
    void youngDriverInSuvWithOneClaimAndGoodScoreIsSurcharged() {
        // 1000 x 1.60 (age) x 1.15 (SUV) x 1.20 (1 claim) x 0.90 (score >= 750)
        BigDecimal premium = engine.rate(new BigDecimal("1000.00"), 22, VehicleType.SUV, 1, OptionalInt.of(780));
        assertThat(premium).isEqualByComparingTo("1987.20");
    }

    @Test
    void missingScoreIsNeutral() {
        BigDecimal premium = engine.rate(new BigDecimal("1180.00"), 40, VehicleType.SEDAN, 0, OptionalInt.empty());
        assertThat(premium).isEqualByComparingTo("1180.00");
    }

    @Test
    void roundsHalfUpToCents() {
        BigDecimal premium = engine.rate(new BigDecimal("999.99"), 30, VehicleType.SUV, 0, OptionalInt.of(700));
        assertThat(premium).isEqualByComparingTo("1149.99");
    }

    @Test
    void quoteCannotBeBoundBeforeItIsRated() {
        Quote draft = new Quote("Test Driver", 30, "TX", VehicleType.SEDAN, 0);
        assertThatThrownBy(() -> draft.bind(Instant.now()))
                .isInstanceOf(QuoteRuleException.class)
                .hasMessageContaining("DRAFT");
        assertThat(draft.getStatus()).isEqualTo(QuoteStatus.DRAFT);
    }
}
