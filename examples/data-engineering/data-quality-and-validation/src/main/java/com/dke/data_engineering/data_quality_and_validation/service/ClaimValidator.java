package com.dke.data_engineering.data_quality_and_validation.service;

import com.dke.data_engineering.data_quality_and_validation.domain.RawClaim;
import com.dke.data_engineering.data_quality_and_validation.domain.Severity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Row-level data quality rules for the claims feed. Pure logic: no database, no I/O. */
@Component
public class ClaimValidator {

    public static final List<String> EXPECTED_HEADER =
            List.of("claim_id", "policy_number", "loss_date", "claim_amount", "currency");
    private static final Pattern POLICY_NUMBER = Pattern.compile("^POL-\\d{6}$");
    private static final Set<String> CURRENCIES = Set.of("GBP", "EUR", "USD");

    public record Finding(int lineNumber, String ruleCode, Severity severity, String message, String rawLine) { }

    public record ValidRow(String claimId, String policyNumber, LocalDate lossDate, BigDecimal amount, String currency) { }

    public record Result(List<ValidRow> valid, List<Finding> findings, int rowsRejected, int rowsWarned) { }

    private final BigDecimal highAmountThreshold;

    public ClaimValidator(@Value("${dq.high-amount-threshold}") BigDecimal highAmountThreshold) {
        this.highAmountThreshold = highAmountThreshold;
    }

    /** Runs every rule on every row. A row with any ERROR is rejected; WARN rows are kept. */
    public Result validate(List<RawClaim> rows, LocalDate businessDate) {
        List<ValidRow> valid = new ArrayList<>();
        List<Finding> findings = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int rejected = 0;
        int warned = 0;

        for (RawClaim row : rows) {
            List<Finding> rowFindings = new ArrayList<>();
            String claimId = trim(row.claimId());
            if (claimId.isEmpty()) {
                rowFindings.add(error(row, "CLAIM_ID_REQUIRED", "claim_id is blank"));
            } else if (!seenIds.add(claimId)) {
                rowFindings.add(error(row, "CLAIM_ID_UNIQUE", "claim_id " + claimId + " repeats in this file"));
            }
            String policy = trim(row.policyNumber());
            if (!POLICY_NUMBER.matcher(policy).matches()) {
                rowFindings.add(error(row, "POLICY_NUMBER_FORMAT", "policy_number '" + policy + "' is not POL-nnnnnn"));
            }
            LocalDate lossDate = parseDate(row.lossDate());
            if (lossDate == null) {
                rowFindings.add(error(row, "LOSS_DATE_VALID", "loss_date '" + row.lossDate() + "' is not yyyy-MM-dd"));
            } else if (lossDate.isAfter(businessDate)) {
                rowFindings.add(error(row, "LOSS_DATE_VALID", "loss_date " + lossDate + " is after file date " + businessDate));
            }
            BigDecimal amount = parseAmount(row.amount());
            if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
                rowFindings.add(error(row, "AMOUNT_POSITIVE", "claim_amount '" + row.amount() + "' must be > 0 with at most 2 decimals"));
            }
            String currency = trim(row.currency()).toUpperCase();
            if (!CURRENCIES.contains(currency)) {
                rowFindings.add(error(row, "CURRENCY_ALLOWED", "currency '" + row.currency() + "' is not one of " + CURRENCIES));
            }

            boolean hasError = !rowFindings.isEmpty();
            if (!hasError && amount.compareTo(highAmountThreshold) > 0) {
                rowFindings.add(new Finding(row.lineNumber(), "HIGH_AMOUNT", Severity.WARN,
                        "claim_amount " + amount + " exceeds review threshold " + highAmountThreshold, row.rawLine()));
                warned++;
            }
            findings.addAll(rowFindings);
            if (hasError) {
                rejected++;
            } else {
                valid.add(new ValidRow(claimId, policy, lossDate, amount, currency));
            }
        }
        return new Result(valid, findings, rejected, warned);
    }

    private static Finding error(RawClaim row, String code, String message) {
        return new Finding(row.lineNumber(), code, Severity.ERROR, message, row.rawLine());
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(trim(s));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal parseAmount(String s) {
        try {
            return new BigDecimal(trim(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
