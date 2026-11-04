package com.dke.banking.payments_processing_ach_wire_rtp.repository;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);
}
