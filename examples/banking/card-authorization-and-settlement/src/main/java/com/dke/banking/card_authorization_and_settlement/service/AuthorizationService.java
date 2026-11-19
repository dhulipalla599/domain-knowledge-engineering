package com.dke.banking.card_authorization_and_settlement.service;

import com.dke.banking.card_authorization_and_settlement.domain.Authorization;
import com.dke.banking.card_authorization_and_settlement.domain.CardAccount;
import com.dke.banking.card_authorization_and_settlement.domain.CardEvent;
import com.dke.banking.card_authorization_and_settlement.domain.CardStatus;
import com.dke.banking.card_authorization_and_settlement.integration.FraudScorer;
import com.dke.banking.card_authorization_and_settlement.repository.AuthorizationRepository;
import com.dke.banking.card_authorization_and_settlement.repository.CardAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issuer-side business rules: approve or decline, hold funds, reverse, and clear. */
@Service
public class AuthorizationService {

    static final int FRAUD_DECLINE_SCORE = 900;
    /** Illustrative over-clearing tolerance by merchant category (e.g. restaurant tips). Real values come from network rules. */
    static final Map<String, BigDecimal> CLEARING_TOLERANCE = Map.of("5812", new BigDecimal("0.20"));

    private final CardAccountRepository cards;
    private final AuthorizationRepository authorizations;
    private final FraudScorer fraudScorer;
    private final ApplicationEventPublisher events;

    public AuthorizationService(CardAccountRepository cards, AuthorizationRepository authorizations,
                                FraudScorer fraudScorer, ApplicationEventPublisher events) {
        this.cards = cards;
        this.authorizations = authorizations;
        this.fraudScorer = fraudScorer;
        this.events = events;
    }

    @Transactional
    public Authorization authorize(String networkReference, String cardToken, String merchantName,
                                   String mcc, BigDecimal amount, String currency) {
        var existing = authorizations.findByNetworkReference(networkReference);
        if (existing.isPresent()) {
            if (!existing.get().sameRequest(cardToken, amount, currency)) {
                throw new CardRuleException(HttpStatus.CONFLICT,
                        "Network reference " + networkReference + " was already used for a different request");
            }
            return existing.get(); // network retry: answer exactly as before, never hold twice
        }
        CardAccount card = findCard(cardToken);
        if (!card.getCurrency().equals(currency)) {
            throw new CardRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Card is billed in " + card.getCurrency() + "; " + currency + " needs currency conversion first");
        }

        String responseCode = decide(card, mcc, amount);
        Authorization auth;
        if ("00".equals(responseCode)) {
            card.placeHold(amount);
            auth = Authorization.approved(networkReference, cardToken, merchantName, mcc, amount, currency,
                    approvalCode(networkReference));
            authorizations.save(auth);
            events.publishEvent(new CardEvent.AuthorizationApproved(auth.getId(), cardToken, amount, mcc));
        } else {
            auth = Authorization.declined(networkReference, cardToken, merchantName, mcc, amount, currency,
                    responseCode);
            authorizations.save(auth);
            events.publishEvent(new CardEvent.AuthorizationDeclined(auth.getId(), cardToken, responseCode));
        }
        return auth;
    }

    /** ISO 8583-style response code: 00 approve, 62 restricted card, 59 suspected fraud, 51 insufficient funds. */
    String decide(CardAccount card, String mcc, BigDecimal amount) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            return "62";
        }
        if (fraudScorer.score(card.getCardToken(), mcc, amount) >= FRAUD_DECLINE_SCORE) {
            return "59";
        }
        if (amount.compareTo(card.availableCredit()) > 0) {
            return "51";
        }
        return "00";
    }

    @Transactional
    public Authorization reverse(UUID authorizationId) {
        Authorization auth = findAuthorization(authorizationId);
        auth.reverse();
        findCard(auth.getCardToken()).releaseHold(auth.getAmount());
        events.publishEvent(new CardEvent.AuthorizationReversed(auth.getId(), auth.getCardToken(), auth.getAmount()));
        return auth;
    }

    @Transactional
    public Authorization clear(UUID authorizationId, BigDecimal clearedAmount) {
        Authorization auth = findAuthorization(authorizationId);
        BigDecimal tolerance = CLEARING_TOLERANCE.getOrDefault(auth.getMcc(), BigDecimal.ZERO);
        BigDecimal maxClearing = auth.getAmount().multiply(BigDecimal.ONE.add(tolerance));
        if (clearedAmount.compareTo(maxClearing) > 0) {
            throw new CardRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Clearing amount " + clearedAmount + " exceeds authorized " + auth.getAmount()
                            + " plus tolerance for MCC " + auth.getMcc());
        }
        auth.clear(clearedAmount);
        findCard(auth.getCardToken()).postCleared(auth.getAmount(), clearedAmount);
        events.publishEvent(new CardEvent.TransactionCleared(auth.getId(), auth.getCardToken(),
                auth.getAmount(), clearedAmount));
        return auth;
    }

    @Transactional(readOnly = true)
    public CardAccount findCard(String cardToken) {
        return cards.findById(cardToken).orElseThrow(() ->
                new CardRuleException(HttpStatus.NOT_FOUND, "Unknown card " + cardToken));
    }

    @Transactional(readOnly = true)
    public Authorization findAuthorization(UUID id) {
        return authorizations.findById(id).orElseThrow(() ->
                new CardRuleException(HttpStatus.NOT_FOUND, "Unknown authorization " + id));
    }

    @Transactional(readOnly = true)
    public List<Authorization> historyFor(String cardToken) {
        findCard(cardToken);
        return authorizations.findByCardTokenOrderByCreatedAtDesc(cardToken);
    }

    private static String approvalCode(String networkReference) {
        return "%06d".formatted(Math.floorMod(networkReference.hashCode(), 1_000_000));
    }
}
