package com.dke.banking.customer_onboarding_and_kyc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.banking.customer_onboarding_and_kyc.domain.ApplicationStatus;
import com.dke.banking.customer_onboarding_and_kyc.domain.BusinessRuleViolation;
import com.dke.banking.customer_onboarding_and_kyc.domain.Decision;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingApplication;
import com.dke.banking.customer_onboarding_and_kyc.domain.RiskRating;
import com.dke.banking.customer_onboarding_and_kyc.domain.ScreeningOutcome;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ApplicationLifecycleTest {

    private OnboardingApplication newApplication() {
        var app = new OnboardingApplication("key", "Priya Raman", LocalDate.of(1991, 4, 12), "IN", "Engineer");
        app.startScreening();
        return app;
    }

    @Test
    void reviewNeedsAReasonAndCanApprove() {
        var app = newApplication();
        app.completeScreening(ScreeningOutcome.POSSIBLE_MATCH, RiskRating.LOW, Decision.REVIEW);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.IN_REVIEW);

        assertThatThrownBy(() -> app.resolveReview(true, " "))
                .isInstanceOf(BusinessRuleViolation.class);

        app.resolveReview(true, "Different date of birth and nationality from the listed person");
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void finalStatusesCannotChange() {
        var app = newApplication();
        app.completeScreening(ScreeningOutcome.CONFIRMED_MATCH, RiskRating.LOW, Decision.REJECT);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(app.getStatus().customerFacing()).isEqualTo("DECLINED");
        assertThatThrownBy(() -> app.resolveReview(true, "late override"))
                .isInstanceOf(BusinessRuleViolation.class);
    }
}
