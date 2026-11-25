package com.dke.insurance.billing_and_premium_collection.api;

import com.dke.insurance.billing_and_premium_collection.repository.InstallmentRepository;
import com.dke.insurance.billing_and_premium_collection.service.BillingService;
import com.dke.insurance.billing_and_premium_collection.service.BillingService.OverdueRunResult;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BillingController {

    private final BillingService billing;
    private final InstallmentRepository installments;

    public BillingController(BillingService billing, InstallmentRepository installments) {
        this.billing = billing;
        this.installments = installments;
    }

    @GetMapping("/installments")
    public List<InstallmentResponse> list(@RequestParam String policyNumber) {
        return installments.findByPolicyNumberOrderBySequenceNo(policyNumber).stream()
                .map(InstallmentResponse::from).toList();
    }

    @GetMapping("/installments/{id}")
    public InstallmentResponse get(@PathVariable Long id) {
        return InstallmentResponse.from(billing.find(id));
    }

    /** 201 for a new payment, 200 when the Idempotency-Key replays an earlier one. */
    @PostMapping("/installments/{id}/payments")
    public ResponseEntity<PaymentResponse> pay(@PathVariable Long id,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody PaymentRequest request) {
        var outcome = billing.recordPayment(id, idempotencyKey, request.amount(), request.paymentToken());
        var status = outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PaymentResponse.from(outcome));
    }

    @PostMapping("/billing-runs/overdue")
    public OverdueRunResult runOverdue(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return billing.runOverdueCheck(asOf);
    }
}
