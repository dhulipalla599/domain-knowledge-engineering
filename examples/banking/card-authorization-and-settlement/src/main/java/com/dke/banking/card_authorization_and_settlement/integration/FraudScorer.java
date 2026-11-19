package com.dke.banking.card_authorization_and_settlement.integration;

import java.math.BigDecimal;

/** Real-time fraud model called during authorization. Returns a risk score from 0 to 999. */
public interface FraudScorer {

    int score(String cardToken, String mcc, BigDecimal amount);
}
