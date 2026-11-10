package com.dke.insurance.policy_administration.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pro-rata (daily) premium arithmetic: the share of an annual amount that covers effective..expiry. */
public final class ProRata {

    private ProRata() {
    }

    public static BigDecimal remainingShare(BigDecimal annualAmount, LocalDate inception,
                                            LocalDate expiry, LocalDate effective) {
        long termDays = ChronoUnit.DAYS.between(inception, expiry);
        long remainingDays = ChronoUnit.DAYS.between(effective, expiry);
        return annualAmount.multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(termDays), 2, RoundingMode.HALF_UP);
    }
}
