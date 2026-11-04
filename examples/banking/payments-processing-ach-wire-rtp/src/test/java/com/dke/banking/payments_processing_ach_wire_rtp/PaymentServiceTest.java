package com.dke.banking.payments_processing_ach_wire_rtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.Account;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentStatus;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.Rail;
import com.dke.banking.payments_processing_ach_wire_rtp.integration.PaymentNetwork;
import com.dke.banking.payments_processing_ach_wire_rtp.integration.StubPaymentNetwork;
import com.dke.banking.payments_processing_ach_wire_rtp.integration.StubSanctionsScreener;
import com.dke.banking.payments_processing_ach_wire_rtp.repository.AccountRepository;
import com.dke.banking.payments_processing_ach_wire_rtp.repository.PaymentOrderRepository;
import com.dke.banking.payments_processing_ach_wire_rtp.service.PaymentRuleException;
import com.dke.banking.payments_processing_ach_wire_rtp.service.PaymentService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PaymentServiceTest {

    private final PaymentOrderRepository orders = mock(PaymentOrderRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PaymentNetwork network = new StubPaymentNetwork();
    private PaymentService service;
    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account("ACC-1", "Test Co", new BigDecimal("1000.00"));
        when(accounts.findById("ACC-1")).thenReturn(Optional.of(account));
        when(orders.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orders.save(any(PaymentOrder.class))).thenAnswer(i -> i.getArgument(0));
        service = new PaymentService(orders, accounts, new StubSanctionsScreener(), network,
                mock(ApplicationEventPublisher.class));
    }

    private PaymentOrder order(String key, Rail rail, String name, String routing, String amount) {
        return new PaymentOrder(key, "ACC-1", rail, name, routing, "123456", new BigDecimal(amount));
    }

    @Test
    void rtpPaymentSettlesImmediatelyAndDebitsAccount() {
        PaymentOrder result = service.initiate(order("k1", Rail.RTP, "Acme", "021000021", "400.00"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(account.getBalance()).isEqualByComparingTo("600.00");
        assertThat(account.getHeld()).isEqualByComparingTo("0.00");
    }

    @Test
    void achStaysSubmittedWithFundsHeldUntilSettlement() {
        PaymentOrder result = service.initiate(order("k2", Rail.ACH, "Acme", "021000021", "400.00"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUBMITTED);
        assertThat(account.available()).isEqualByComparingTo("600.00");
        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void rejectsInsufficientFunds() {
        assertThatThrownBy(() -> service.initiate(order("k3", Rail.WIRE, "Acme", "021000021", "1000.01")))
                .isInstanceOf(PaymentRuleException.class).hasMessageContaining("Insufficient");
    }

    @Test
    void rejectsAmountOverRailLimit() {
        account.credit(new BigDecimal("500000"));
        assertThatThrownBy(() -> service.initiate(order("k4", Rail.RTP, "Acme", "021000021", "100000.01")))
                .isInstanceOf(PaymentRuleException.class).hasMessageContaining("limit");
    }

    @Test
    void rejectsSanctionedBeneficiary() {
        assertThatThrownBy(() -> service.initiate(order("k5", Rail.WIRE, "Blocked Trading Ltd", "021000021", "10.00")))
                .isInstanceOf(PaymentRuleException.class).hasMessageContaining("sanctions");
    }

    @Test
    void networkRefusalReleasesTheHold() {
        PaymentOrder result = service.initiate(order("k6", Rail.ACH, "Acme", "000000000", "100.00"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(account.available()).isEqualByComparingTo("1000.00");
    }

    @Test
    void sameKeySameInstructionReturnsExistingOrder() {
        PaymentOrder first = order("k7", Rail.RTP, "Acme", "021000021", "10.00");
        when(orders.findByIdempotencyKey("k7")).thenReturn(Optional.of(first));
        assertThat(service.initiate(order("k7", Rail.RTP, "Acme", "021000021", "10.00"))).isSameAs(first);
        assertThatThrownBy(() -> service.initiate(order("k7", Rail.RTP, "Acme", "021000021", "11.00")))
                .isInstanceOf(PaymentRuleException.class).hasMessageContaining("Idempotency");
    }

    @Test
    void illegalStateTransitionIsRefused() {
        PaymentOrder o = order("k8", Rail.RTP, "Acme", "021000021", "10.00");
        assertThatThrownBy(o::settle).isInstanceOf(IllegalStateException.class);
    }
}
