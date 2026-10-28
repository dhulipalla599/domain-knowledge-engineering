package com.dke.insurance.underwriting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PolicyApplicationTest {

    private PolicyApplication newApplication() {
        return new PolicyApplication("k", "Test", new BigDecimal("100000"), 2000, false, 0, 10);
    }

    @Test
    void referredCaseCanBeApproved() {
        var app = newApplication();
        app.moveTo(ApplicationStatus.REFERRED, "needs review", "auto-rules");
        app.moveTo(ApplicationStatus.APPROVED, "ok", "uw-senior");
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(app.getDecidedBy()).isEqualTo("uw-senior");
    }

    @Test
    void decidedCaseIsFinal() {
        var app = newApplication();
        app.moveTo(ApplicationStatus.DECLINED, "no", "auto-rules");
        assertThatThrownBy(() -> app.moveTo(ApplicationStatus.APPROVED, "changed mind", "uw-senior"))
                .isInstanceOf(IllegalTransitionException.class);
    }
}
