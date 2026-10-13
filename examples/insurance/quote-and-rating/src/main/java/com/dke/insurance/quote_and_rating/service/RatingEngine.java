package com.dke.insurance.quote_and_rating.service;

import com.dke.insurance.quote_and_rating.domain.VehicleType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/** Pure premium calculation: no I/O, so it is trivially unit-testable and reproducible for audit. */
@Component
public class RatingEngine {

    public BigDecimal rate(BigDecimal baseAmount, int driverAge, VehicleType vehicle, int priorClaims,
                           OptionalInt insuranceScore) {
        BigDecimal premium = baseAmount
                .multiply(ageFactor(driverAge))
                .multiply(vehicle.factor())
                .multiply(BigDecimal.ONE.add(new BigDecimal("0.20").multiply(BigDecimal.valueOf(priorClaims))))
                .multiply(scoreFactor(insuranceScore));
        return premium.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal ageFactor(int age) {
        if (age < 25) return new BigDecimal("1.60");
        if (age >= 65) return new BigDecimal("1.20");
        return BigDecimal.ONE;
    }

    /** No score (state prohibits using it) is neutral. */
    private BigDecimal scoreFactor(OptionalInt score) {
        if (score.isEmpty()) return BigDecimal.ONE;
        if (score.getAsInt() >= 750) return new BigDecimal("0.90");
        if (score.getAsInt() >= 650) return BigDecimal.ONE;
        return new BigDecimal("1.15");
    }
}
