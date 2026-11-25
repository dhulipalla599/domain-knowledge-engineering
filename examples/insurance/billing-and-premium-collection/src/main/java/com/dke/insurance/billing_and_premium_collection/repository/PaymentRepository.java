package com.dke.insurance.billing_and_premium_collection.repository;

import com.dke.insurance.billing_and_premium_collection.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
