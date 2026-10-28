package com.dke.insurance.underwriting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dke.insurance.underwriting.domain.ApplicationStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskScorerTest {

    private final RiskScorer scorer = new RiskScorer();

    @Test
    void cleanModestRiskIsApprovedAutomatically() {
        var result = scorer.assess(new BigDecimal("300000"), 2010, false, 0);
        assertThat(result.outcome()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(result.score()).isEqualTo(10);
    }

    @Test
    void floodZoneOldBuildingIsReferred() {
        var result = scorer.assess(new BigDecimal("300000"), 1940, true, 0);
        assertThat(result.score()).isEqualTo(50);
        assertThat(result.outcome()).isEqualTo(ApplicationStatus.REFERRED);
    }

    @Test
    void largeSumInsuredIsReferredEvenWithCleanHistory() {
        var result = scorer.assess(new BigDecimal("800000"), 2015, false, 0);
        assertThat(result.outcome()).isEqualTo(ApplicationStatus.REFERRED);
    }

    @Test
    void frequentClaimsAreDeclined() {
        var result = scorer.assess(new BigDecimal("200000"), 2015, false, 4);
        assertThat(result.outcome()).isEqualTo(ApplicationStatus.DECLINED);
    }
}
