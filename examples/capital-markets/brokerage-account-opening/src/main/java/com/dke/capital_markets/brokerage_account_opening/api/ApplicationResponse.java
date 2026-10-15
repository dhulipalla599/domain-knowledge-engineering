package com.dke.capital_markets.brokerage_account_opening.api;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountApplication;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountType;
import com.dke.capital_markets.brokerage_account_opening.domain.ApplicationStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ApplicationResponse(UUID id, String applicantName, AccountType accountType,
                                  BigDecimal liquidNetWorth, ApplicationStatus status,
                                  String decisionReason, String accountNumber) {

    static ApplicationResponse from(AccountApplication a) {
        return new ApplicationResponse(a.getId(), a.getApplicantName(), a.getAccountType(),
                a.getLiquidNetWorth(), a.getStatus(), a.getDecisionReason(), a.getAccountNumber());
    }
}
