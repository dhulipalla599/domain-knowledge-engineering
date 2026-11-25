package com.dke.insurance.billing_and_premium_collection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InstallmentTest {

    private Installment installment() {
        return Installment.bill("HM-TEST-1", 1, new BigDecimal("100.00"), LocalDate.of(2026, 12, 1), 30);
    }

    @Test
    void partialThenFullPaymentSettlesTheInstallment() {
        var i = installment();
        i.applyPayment(new BigDecimal("40.00"));
        assertThat(i.getStatus()).isEqualTo(InstallmentStatus.PARTIALLY_PAID);
        assertThat(i.outstanding()).isEqualByComparingTo("60.00");
        i.applyPayment(new BigDecimal("60.00"));
        assertThat(i.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(i.outstanding()).isEqualByComparingTo("0.00");
    }

    @Test
    void partialPaymentDuringGraceKeepsItOverdue() {
        var i = installment();
        i.markOverdue();
        i.applyPayment(new BigDecimal("10.00"));
        assertThat(i.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
    }

    @Test
    void graceEndsThirtyDaysAfterDueDate() {
        var i = installment();
        assertThat(i.getGraceEndsOn()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(i.isPastGrace(LocalDate.of(2026, 12, 31))).isFalse();
        assertThat(i.isPastGrace(LocalDate.of(2027, 1, 1))).isTrue();
    }

    @Test
    void defaultedAndPaidInstallmentsRejectPayments() {
        var i = installment();
        i.markOverdue();
        i.markDefaulted();
        assertThatThrownBy(i::requirePayable).isInstanceOf(IllegalTransitionException.class);
        assertThatThrownBy(() -> i.applyPayment(BigDecimal.ONE)).isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void cannotDefaultWithoutBeingOverdueFirst() {
        assertThatThrownBy(installment()::markDefaulted)
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("BILLED -> DEFAULTED");
    }
}
