package com.dke.banking.payments_processing_ach_wire_rtp.api;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;
import com.dke.banking.payments_processing_ach_wire_rtp.service.PaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                  @Valid @RequestBody PaymentRequest request) {
        PaymentOrder order = service.initiate(new PaymentOrder(idempotencyKey, request.fromAccount(),
                request.rail(), request.beneficiaryName(), request.beneficiaryRouting(),
                request.beneficiaryAccount(), request.amount()));
        return ResponseEntity.created(URI.create("/api/payments/" + order.getId()))
                .body(PaymentResponse.from(order));
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(service.find(id));
    }

    @PostMapping("/payments/{id}/settle")
    public PaymentResponse settle(@PathVariable UUID id) {
        return PaymentResponse.from(service.settle(id));
    }

    @PostMapping("/payments/{id}/return")
    public PaymentResponse returnPayment(@PathVariable UUID id,
                                         @RequestParam(defaultValue = "R01 Insufficient funds") String reason) {
        return PaymentResponse.from(service.returnPayment(id, reason));
    }

    @GetMapping("/accounts/{accountNumber}")
    public AccountResponse account(@PathVariable String accountNumber) {
        return AccountResponse.from(service.findAccount(accountNumber));
    }
}
