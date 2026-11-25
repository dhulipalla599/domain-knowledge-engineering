package com.dke.data_engineering.data_quality_and_validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dke.data_engineering.data_quality_and_validation.domain.RawClaim;
import com.dke.data_engineering.data_quality_and_validation.domain.Severity;
import com.dke.data_engineering.data_quality_and_validation.service.ClaimValidator;
import com.dke.data_engineering.data_quality_and_validation.service.ClaimValidator.Finding;
import com.dke.data_engineering.data_quality_and_validation.service.ClaimValidator.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaimValidatorTest {

    private final ClaimValidator validator = new ClaimValidator(new BigDecimal("250000"));
    private final LocalDate fileDate = LocalDate.parse("2026-11-24");

    private static RawClaim row(int line, String id, String policy, String date, String amount, String ccy) {
        return new RawClaim(line, id, policy, date, amount, ccy, String.join(",", id, policy, date, amount, ccy));
    }

    @Test
    void cleanRowIsValid() {
        Result r = validator.validate(List.of(row(2, "CLM-1", "POL-123456", "2026-11-20", "100.50", "gbp")), fileDate);
        assertThat(r.rowsRejected()).isZero();
        assertThat(r.valid()).singleElement().satisfies(v -> assertThat(v.currency()).isEqualTo("GBP"));
    }

    @Test
    void rowWithErrorsIsRejectedAndEveryFailedRuleIsReported() {
        Result r = validator.validate(List.of(row(2, "CLM-1", "POL12", "2026-12-01", "-5", "JPY")), fileDate);
        assertThat(r.rowsRejected()).isEqualTo(1);
        assertThat(r.valid()).isEmpty();
        assertThat(r.findings()).extracting(Finding::ruleCode)
                .containsExactly("POLICY_NUMBER_FORMAT", "LOSS_DATE_VALID", "AMOUNT_POSITIVE", "CURRENCY_ALLOWED");
    }

    @Test
    void duplicateClaimIdKeepsFirstAndRejectsRepeat() {
        Result r = validator.validate(List.of(
                row(2, "CLM-1", "POL-123456", "2026-11-20", "100.00", "GBP"),
                row(3, "CLM-1", "POL-123456", "2026-11-20", "100.00", "GBP")), fileDate);
        assertThat(r.valid()).hasSize(1);
        assertThat(r.findings()).singleElement().satisfies(f -> {
            assertThat(f.ruleCode()).isEqualTo("CLAIM_ID_UNIQUE");
            assertThat(f.lineNumber()).isEqualTo(3);
        });
    }

    @Test
    void highAmountIsAWarningNotARejection() {
        Result r = validator.validate(List.of(row(2, "CLM-9", "POL-654321", "2026-11-20", "300000.00", "EUR")), fileDate);
        assertThat(r.valid()).hasSize(1);
        assertThat(r.rowsWarned()).isEqualTo(1);
        assertThat(r.findings()).singleElement().extracting(Finding::severity).isEqualTo(Severity.WARN);
    }

    @Test
    void amountWithMoreThanTwoDecimalsIsRejected() {
        Result r = validator.validate(List.of(row(2, "CLM-2", "POL-123456", "2026-11-20", "10.005", "USD")), fileDate);
        assertThat(r.findings()).extracting(Finding::ruleCode).containsExactly("AMOUNT_POSITIVE");
    }
}
