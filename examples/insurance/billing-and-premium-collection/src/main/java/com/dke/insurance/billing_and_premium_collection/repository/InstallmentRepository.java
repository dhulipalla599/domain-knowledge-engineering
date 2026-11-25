package com.dke.insurance.billing_and_premium_collection.repository;

import com.dke.insurance.billing_and_premium_collection.domain.Installment;
import com.dke.insurance.billing_and_premium_collection.domain.InstallmentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByPolicyNumberOrderBySequenceNo(String policyNumber);

    List<Installment> findByStatusIn(Collection<InstallmentStatus> statuses);
}
