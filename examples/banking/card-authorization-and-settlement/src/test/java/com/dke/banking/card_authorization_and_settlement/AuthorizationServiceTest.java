package com.dke.banking.card_authorization_and_settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dke.banking.card_authorization_and_settlement.domain.Authorization;
import com.dke.banking.card_authorization_and_settlement.domain.AuthorizationStatus;
import com.dke.banking.card_authorization_and_settlement.domain.CardAccount;
import com.dke.banking.card_authorization_and_settlement.repository.AuthorizationRepository;
import com.dke.banking.card_authorization_and_settlement.repository.CardAccountRepository;
import com.dke.banking.card_authorization_and_settlement.service.AuthorizationService;
import com.dke.banking.card_authorization_and_settlement.service.CardRuleException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AuthorizationServiceTest {

    private final CardAccountRepository cards = mock(CardAccountRepository.class);
    private final AuthorizationRepository auths = mock(AuthorizationRepository.class);
    private final AuthorizationService service = new AuthorizationService(cards, auths,
            (token, mcc, amount) -> "7995".equals(mcc) ? 950 : 10, mock(ApplicationEventPublisher.class));
    private CardAccount card;

    @BeforeEach
    void setUp() {
        card = new CardAccount("tok_test", "Test Holder", "USD", new BigDecimal("500.00"));
        when(cards.findById("tok_test")).thenReturn(Optional.of(card));
        when(auths.findByNetworkReference(any())).thenReturn(Optional.empty());
        when(auths.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void approvalPlacesHoldAndReducesAvailableCredit() {
        Authorization a = service.authorize("R1", "tok_test", "Shop", "5411", new BigDecimal("200.00"), "USD");
        assertThat(a.getStatus()).isEqualTo(AuthorizationStatus.APPROVED);
        assertThat(a.getResponseCode()).isEqualTo("00");
        assertThat(card.availableCredit()).isEqualByComparingTo("300.00");
    }

    @Test
    void amountAboveAvailableCreditIsDeclinedWithCode51AndNoHold() {
        Authorization a = service.authorize("R2", "tok_test", "Shop", "5411", new BigDecimal("500.01"), "USD");
        assertThat(a.getStatus()).isEqualTo(AuthorizationStatus.DECLINED);
        assertThat(a.getResponseCode()).isEqualTo("51");
        assertThat(card.getPendingHolds()).isEqualByComparingTo("0");
    }

    @Test
    void highFraudScoreIsDeclinedWithCode59() {
        Authorization a = service.authorize("R3", "tok_test", "Casino", "7995", new BigDecimal("10.00"), "USD");
        assertThat(a.getResponseCode()).isEqualTo("59");
    }

    @Test
    void restaurantMayClearUpToTwentyPercentAboveAuthorizationButNotMore() {
        Authorization tip = service.authorize("R4", "tok_test", "Bistro", "5812", new BigDecimal("100.00"), "USD");
        when(auths.findById(tip.getId())).thenReturn(Optional.of(tip));
        service.clear(tip.getId(), new BigDecimal("120.00"));
        assertThat(card.getPostedBalance()).isEqualByComparingTo("120.00");
        assertThat(card.getPendingHolds()).isEqualByComparingTo("0");

        Authorization shop = service.authorize("R5", "tok_test", "Shop", "5411", new BigDecimal("50.00"), "USD");
        when(auths.findById(shop.getId())).thenReturn(Optional.of(shop));
        assertThatThrownBy(() -> service.clear(shop.getId(), new BigDecimal("50.01")))
                .isInstanceOf(CardRuleException.class).hasMessageContaining("tolerance");
    }

    @Test
    void reversedAuthorizationCannotBeCleared() {
        Authorization a = service.authorize("R6", "tok_test", "Shop", "5411", new BigDecimal("40.00"), "USD");
        a.reverse();
        assertThatThrownBy(() -> a.clear(new BigDecimal("40.00"))).isInstanceOf(IllegalStateException.class);
    }
}
