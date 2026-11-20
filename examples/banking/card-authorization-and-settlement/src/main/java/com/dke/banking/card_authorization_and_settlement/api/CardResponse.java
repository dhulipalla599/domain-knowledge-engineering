package com.dke.banking.card_authorization_and_settlement.api;

import com.dke.banking.card_authorization_and_settlement.domain.CardAccount;
import com.dke.banking.card_authorization_and_settlement.domain.CardStatus;
import java.math.BigDecimal;

public record CardResponse(String cardToken, String holderName, String currency, CardStatus status,
                           BigDecimal creditLimit, BigDecimal postedBalance, BigDecimal pendingHolds,
                           BigDecimal availableCredit) {

    static CardResponse from(CardAccount c) {
        return new CardResponse(c.getCardToken(), c.getHolderName(), c.getCurrency(), c.getStatus(),
                c.getCreditLimit(), c.getPostedBalance(), c.getPendingHolds(), c.availableCredit());
    }
}
