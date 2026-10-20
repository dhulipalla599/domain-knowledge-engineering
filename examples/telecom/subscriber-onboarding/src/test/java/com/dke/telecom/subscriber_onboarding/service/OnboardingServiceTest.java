package com.dke.telecom.subscriber_onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dke.telecom.subscriber_onboarding.api.OnboardingException;
import com.dke.telecom.subscriber_onboarding.api.OnboardingRequest;
import com.dke.telecom.subscriber_onboarding.domain.*;
import com.dke.telecom.subscriber_onboarding.integration.IdentityVerifier;
import com.dke.telecom.subscriber_onboarding.repository.PhoneNumberRepository;
import com.dke.telecom.subscriber_onboarding.repository.SimCardRepository;
import com.dke.telecom.subscriber_onboarding.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OnboardingServiceTest {

    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final SimCardRepository simCards = mock(SimCardRepository.class);
    private final PhoneNumberRepository numbers = mock(PhoneNumberRepository.class);
    private final IdentityVerifier verifier = mock(IdentityVerifier.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final OnboardingService service =
            new OnboardingService(subscriptions, simCards, numbers, verifier, events);

    private OnboardingRequest request(LocalDate dob) {
        return new OnboardingRequest("Test User", dob, "ID-1", Plan.PREPAID_BASIC, "8944-1");
    }

    @Test
    void rejectsApplicantsUnder18BeforeTouchingAnyInventory() {
        assertThatThrownBy(() -> service.onboard("k1", request(LocalDate.now().minusYears(17))))
                .isInstanceOf(OnboardingException.class)
                .hasMessageContaining("at least 18");
        verifyNoInteractions(simCards, numbers, verifier);
    }

    @Test
    void rejectsWhenIdAlreadyHasMaximumActiveLines() {
        when(subscriptions.countByIdDocumentNumberAndStatus("ID-1", SubscriptionStatus.ACTIVE))
                .thenReturn((long) OnboardingService.MAX_ACTIVE_LINES_PER_ID);
        assertThatThrownBy(() -> service.onboard("k2", request(LocalDate.of(1990, 1, 1))))
                .isInstanceOf(OnboardingException.class)
                .hasMessageContaining("active lines");
    }

    @Test
    void replayedIdempotencyKeyReturnsExistingSubscription() {
        Subscription existing = Subscription.submit("k3", "Test User", LocalDate.of(1990, 1, 1),
                "ID-1", Plan.PREPAID_BASIC, "8944-1");
        when(subscriptions.findByIdempotencyKey("k3")).thenReturn(Optional.of(existing));

        assertThat(service.onboard("k3", request(LocalDate.of(1990, 1, 1)))).isSameAs(existing);
        verify(subscriptions, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void illegalStateTransitionsAreRefused() {
        Subscription s = Subscription.submit("k4", "Test User", LocalDate.of(1990, 1, 1),
                "ID-1", Plan.PREPAID_BASIC, "8944-1");
        assertThatThrownBy(() -> s.activate("+1555")).isInstanceOf(IllegalTransitionException.class);
    }
}
