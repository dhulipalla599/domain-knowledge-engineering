package com.dke.healthcare.insurance_eligibility_verification.repository;

import com.dke.healthcare.insurance_eligibility_verification.domain.Payer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayerRepository extends JpaRepository<Payer, String> {
}
