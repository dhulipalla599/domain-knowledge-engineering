package com.dke.insurance.billing_and_premium_collection.service;

import com.dke.insurance.billing_and_premium_collection.domain.BillingEvent;
import com.dke.insurance.billing_and_premium_collection.domain.Installment;
import com.dke.insurance.billing_and_premium_collection.domain.InstallmentStatus;
import com.dke.insurance.billing_and_premium_collection.domain.Payment;
import com.dke.insurance.billing_and_premium_collection.integration.PaymentGateway;
import com.dke.insurance.billing_and_premium_collection.repository.InstallmentRepository;
import com.dke.insurance.billing_and_premium_collection.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final InstallmentRepository installments;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BillingService(InstallmentRepository installments, PaymentRepository payments,
                          PaymentGateway gateway, ApplicationEventPublisher events, Clock clock) {
        this.installments = installments;
        this.payments = payments;
        this.gateway = gateway;
        this.events = events;
        this.clock = clock;
    }

    public record PaymentOutcome(Payment payment, Installment installment, boolean replayed) {
    }

    public record OverdueRunResult(LocalDate asOf, int markedOverdue, int markedDefaulted) {
    }

    @Transactional(readOnly = true)
    public Installment find(Long id) {
        return installments.findById(id)
                .orElseThrow(() -> new BillingRuleException(HttpStatus.NOT_FOUND, "Installment " + id + " not found"));
    }

    /**
     * Collects a premium payment against one installment.
     * Rules: same idempotency key never charges twice; installment must be open; no overpayment.
     */
    @Transactional
    public PaymentOutcome recordPayment(Long installmentId, String idempotencyKey, BigDecimal amount, String paymentToken) {
        var previous = payments.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (previous.get().conflictsWith(installmentId, amount)) {
                throw new BillingRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Idempotency key " + idempotencyKey + " was already used for a different payment");
            }
            return new PaymentOutcome(previous.get(), find(installmentId), true);
        }

        var installment = find(installmentId);
        installment.requirePayable();
        if (amount.compareTo(installment.outstanding()) > 0) {
            throw new BillingRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Payment " + amount + " exceeds outstanding premium " + installment.outstanding());
        }

        var result = gateway.charge(paymentToken, amount, idempotencyKey);
        if (!result.approved()) {
            events.publishEvent(new BillingEvent.PaymentFailed(
                    installment.getPolicyNumber(), installmentId, amount, result.declineReason()));
            throw new BillingRuleException(HttpStatus.PAYMENT_REQUIRED, "Payment declined: " + result.declineReason());
        }

        installment.applyPayment(amount);
        var payment = payments.save(new Payment(idempotencyKey, installmentId, amount,
                result.reference(), Instant.now(clock)));
        events.publishEvent(new BillingEvent.PaymentReceived(installment.getPolicyNumber(), installmentId,
                payment.getId(), amount, installment.getStatus()));
        return new PaymentOutcome(payment, installment, false);
    }

    /** Daily job: open installments past their due date become OVERDUE, past grace become DEFAULTED. */
    @Transactional
    public OverdueRunResult runOverdueCheck(LocalDate asOf) {
        int overdue = 0;
        int defaulted = 0;
        for (var i : installments.findByStatusIn(InstallmentStatus.OPEN)) {
            if (i.isPastDue(asOf) && i.getStatus() != InstallmentStatus.OVERDUE) {
                i.markOverdue();
                overdue++;
                events.publishEvent(new BillingEvent.InstallmentOverdue(i.getPolicyNumber(), i.getId(), i.outstanding()));
            }
            if (i.isPastGrace(asOf)) {
                i.markDefaulted();
                defaulted++;
                events.publishEvent(new BillingEvent.InstallmentDefaulted(i.getPolicyNumber(), i.getId(), i.outstanding()));
            }
        }
        return new OverdueRunResult(asOf, overdue, defaulted);
    }
}
