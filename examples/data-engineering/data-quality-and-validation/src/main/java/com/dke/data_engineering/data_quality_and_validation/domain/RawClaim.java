package com.dke.data_engineering.data_quality_and_validation.domain;

/** One untyped row exactly as it arrived in the file; validation happens later. */
public record RawClaim(int lineNumber, String claimId, String policyNumber, String lossDate,
                       String amount, String currency, String rawLine) { }
