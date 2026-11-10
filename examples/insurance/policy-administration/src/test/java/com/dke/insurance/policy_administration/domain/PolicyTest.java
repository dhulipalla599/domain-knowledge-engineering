package com.dke.insurance.policy_administration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.insurance.policy_administration.service.ProRata;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PolicyTest {

    private final LocalDate start = LocalDate.of(2026, 1, 1);

    private Policy newPolicy() {
        return Policy.issue("Q-1", "Ana Silva", new BigDecimal("300000"), new BigDecimal("0.003"), start);
    }

    @Test
    void issuedPolicyIsActiveForOneYearWithRatedPremium() {
        var p = newPolicy();
        assertThat(p.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(p.getExpiry()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(p.getAnnualPremium()).isEqualByComparingTo("900.00");
    }

    @Test
    void proRataRefundCoversOnlyUnexpiredDays() {
        // 365-day term, 184 days left on 2026-07-01 -> 900 * 184 / 365 = 453.70
        var refund = ProRata.remainingShare(new BigDecimal("900.00"), start, start.plusYears(1), LocalDate.of(2026, 7, 1));
        assertThat(refund).isEqualByComparingTo("453.70");
    }

    @Test
    void cancelledPolicyCannotBeCancelledOrEndorsedAgain() {
        var p = newPolicy();
        p.cancel(LocalDate.of(2026, 7, 1), new BigDecimal("453.70"));
        assertThat(p.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThatThrownBy(() -> p.cancel(LocalDate.of(2026, 8, 1), BigDecimal.ONE))
                .isInstanceOf(IllegalTransitionException.class);
        assertThatThrownBy(p::expire).isInstanceOf(IllegalTransitionException.class);
    }
}
