package com.dke.banking.payments_processing_ach_wire_rtp.service;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.Account;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentEvent;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.Rail;
import com.dke.banking.payments_processing_ach_wire_rtp.integration.PaymentNetwork;
import com.dke.banking.payments_processing_ach_wire_rtp.integration.PaymentNetwork.NetworkResponse;
import com.dke.banking.payments_processing_ach_wire_rtp.integration.SanctionsScreener;
import com.dke.banking.payments_processing_ach_wire_rtp.repository.AccountRepository;
import com.dke.banking.payments_processing_ach_wire_rtp.repository.PaymentOrderRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentOrderRepository orders;
    private final AccountRepository accounts;
    private final SanctionsScreener sanctions;
    private final PaymentNetwork network;
    private final ApplicationEventPublisher events;

    public PaymentService(PaymentOrderRepository orders, AccountRepository accounts,
                          SanctionsScreener sanctions, PaymentNetwork network,
                          ApplicationEventPublisher events) {
        this.orders = orders;
        this.accounts = accounts;
        this.sanctions = sanctions;
        this.network = network;
        this.events = events;
    }

    /** Core rule set: idempotency, rail limit, sanctions, funds hold, then network submission. */
    @Transactional
    public PaymentOrder initiate(PaymentOrder candidate) {
        var existing = orders.findByIdempotencyKey(candidate.getIdempotencyKey());
        if (existing.isPresent()) {
            if (existing.get().sameInstructionAs(candidate)) {
                return existing.get();   // safe retry: same key, same instruction
            }
            throw new PaymentRuleException(HttpStatus.CONFLICT,
                    "Idempotency key already used with a different instruction");
        }
        if (candidate.getAmount().compareTo(candidate.getRail().maxAmount()) > 0) {
            throw new PaymentRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Amount exceeds the " + candidate.getRail() + " limit of " + candidate.getRail().maxAmount());
        }
        Account account = accounts.findById(candidate.getFromAccount())
                .orElseThrow(() -> new PaymentRuleException(HttpStatus.NOT_FOUND, "Unknown source account"));
        if (sanctions.isBlocked(candidate.getBeneficiaryName())) {
            throw new PaymentRuleException(HttpStatus.UNPROCESSABLE_ENTITY, "Beneficiary failed sanctions screening");
        }
        if (account.available().compareTo(candidate.getAmount()) < 0) {
            throw new PaymentRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient available balance: " + account.available());
        }

        account.hold(candidate.getAmount());
        candidate.markValidated();
        candidate.markSubmitted();
        NetworkResponse response = network.submit(candidate);
        if (!response.accepted()) {
            candidate.reject(response.reason());
            account.release(candidate.getAmount());
            events.publishEvent(new PaymentEvent.PaymentRejected(candidate.getId(), response.reason()));
        } else {
            events.publishEvent(new PaymentEvent.PaymentSubmitted(
                    candidate.getId(), candidate.getRail(), candidate.getAmount()));
            if (candidate.getRail().settlesImmediately()) {
                settleOrder(candidate, account);
            }
        }
        accounts.save(account);
        return orders.save(candidate);
    }

    /** Called when the network confirms settlement (batch rails such as ACH). */
    @Transactional
    public PaymentOrder settle(UUID id) {
        PaymentOrder order = find(id);
        Account account = accounts.findById(order.getFromAccount()).orElseThrow();
        settleOrder(order, account);
        accounts.save(account);
        return orders.save(order);
    }

    /** The receiving bank returned the payment: money comes back to the customer. */
    @Transactional
    public PaymentOrder returnPayment(UUID id, String reason) {
        PaymentOrder order = find(id);
        Account account = accounts.findById(order.getFromAccount()).orElseThrow();
        order.markReturned(reason);
        account.credit(order.getAmount());
        events.publishEvent(new PaymentEvent.PaymentReturned(order.getId(), reason));
        accounts.save(account);
        return orders.save(order);
    }

    @Transactional(readOnly = true)
    public PaymentOrder find(UUID id) {
        return orders.findById(id)
                .orElseThrow(() -> new PaymentRuleException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional(readOnly = true)
    public Account findAccount(String accountNumber) {
        return accounts.findById(accountNumber)
                .orElseThrow(() -> new PaymentRuleException(HttpStatus.NOT_FOUND, "Unknown account"));
    }

    private void settleOrder(PaymentOrder order, Account account) {
        order.settle();
        account.settle(order.getAmount());
        events.publishEvent(new PaymentEvent.PaymentSettled(order.getId(), order.getRail(), order.getAmount()));
    }
}
