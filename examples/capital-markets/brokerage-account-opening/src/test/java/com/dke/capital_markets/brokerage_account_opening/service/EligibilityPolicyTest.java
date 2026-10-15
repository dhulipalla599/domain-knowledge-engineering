package com.dke.capital_markets.brokerage_account_opening.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EligibilityPolicyTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"), ZoneOffset.UTC);
    private final EligibilityPolicy policy = new EligibilityPolicy(clock);

    @Test
    void rejectsApplicantUnder18() {
        assertThatThrownBy(() -> policy.check(LocalDate.of(2008, 10, 15), AccountType.CASH, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 18");
    }

    @Test
    void acceptsApplicantOnTheirEighteenthBirthday() {
        assertThatCode(() -> policy.check(LocalDate.of(2008, 10, 14), AccountType.CASH, BigDecimal.TEN))
                .doesNotThrowAnyException();
    }

    @Test
    void marginRequiresMinimumNetWorth() {
        assertThatThrownBy(() -> policy.check(LocalDate.of(1980, 1, 1), AccountType.MARGIN, new BigDecimal("24999.99")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Margin");
        assertThatCode(() -> policy.check(LocalDate.of(1980, 1, 1), AccountType.MARGIN, new BigDecimal("25000")))
                .doesNotThrowAnyException();
    }
}
