package com.dke.insurance.underwriting.service;

import com.dke.insurance.underwriting.domain.ApplicationStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pure rules engine: no I/O, so it is trivial to unit test and to explain to an auditor. */
@Component
public class RiskScorer {

    static final BigDecimal REFER_ABOVE = new BigDecimal("750000");
    static final int DECLINE_CLAIMS = 4;
    static final int REFER_SCORE = 40;

    public record Assessment(int score, ApplicationStatus outcome, String reason) {
    }

    public Assessment assess(BigDecimal sumInsured, int yearBuilt, boolean floodZone, int priorClaims) {
        int score = 10
                + (floodZone ? 25 : 0)
                + (yearBuilt < 1950 ? 15 : 0)
                + 10 * priorClaims
                + (sumInsured.compareTo(new BigDecimal("500000")) > 0 ? 10 : 0);

        if (priorClaims >= DECLINE_CLAIMS) {
            return new Assessment(score, ApplicationStatus.DECLINED,
                    priorClaims + " claims in 5 years exceeds the knock-out limit");
        }
        if (sumInsured.compareTo(REFER_ABOVE) > 0) {
            return new Assessment(score, ApplicationStatus.REFERRED, "Sum insured above auto-approval limit");
        }
        if (score >= REFER_SCORE) {
            return new Assessment(score, ApplicationStatus.REFERRED, "Risk score " + score + " needs an underwriter");
        }
        return new Assessment(score, ApplicationStatus.APPROVED, "Within automatic acceptance appetite");
    }
}
