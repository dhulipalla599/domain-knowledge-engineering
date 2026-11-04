package com.dke.banking.payments_processing_ach_wire_rtp.repository;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
