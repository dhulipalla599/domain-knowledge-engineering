package com.dke.capital_markets.brokerage_account_opening.service;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Component;

/**
 * Pure business rules checked before any money-related or external work happens.
 * The thresholds are fictional house policy; real firms set their own within the rules of their regulator.
 */
@Component
public class EligibilityPolicy {

    static final int MINIMUM_AGE = 18;
    static final BigDecimal MARGIN_MINIMUM_NET_WORTH = new BigDecimal("25000");

    private final Clock clock;

    public EligibilityPolicy(Clock clock) {
        this.clock = clock;
    }

    public void check(LocalDate dateOfBirth, AccountType type, BigDecimal liquidNetWorth) {
        int age = Period.between(dateOfBirth, LocalDate.now(clock)).getYears();
        if (age < MINIMUM_AGE) {
            throw new BusinessRuleException("Applicant must be at least " + MINIMUM_AGE + " years old");
        }
        if (type == AccountType.MARGIN && liquidNetWorth.compareTo(MARGIN_MINIMUM_NET_WORTH) < 0) {
            throw new BusinessRuleException(
                    "Margin accounts require liquid net worth of at least " + MARGIN_MINIMUM_NET_WORTH);
        }
    }
}
