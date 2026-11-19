package com.dke.banking.card_authorization_and_settlement.api;

import com.dke.banking.card_authorization_and_settlement.domain.Authorization;
import com.dke.banking.card_authorization_and_settlement.domain.AuthorizationStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record AuthorizationResponse(UUID id, String networkReference, String cardToken, String merchantName,
                                    String mcc, BigDecimal amount, String currency, AuthorizationStatus status,
                                    String responseCode, String approvalCode, BigDecimal clearedAmount) {

    static AuthorizationResponse from(Authorization a) {
        return new AuthorizationResponse(a.getId(), a.getNetworkReference(), a.getCardToken(), a.getMerchantName(),
                a.getMcc(), a.getAmount(), a.getCurrency(), a.getStatus(), a.getResponseCode(),
                a.getApprovalCode(), a.getClearedAmount());
    }
}
