package com.dke.banking.core_banking_ledger_and_accounts.repository;

import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerLineRepository extends JpaRepository<LedgerLine, UUID> {

    List<LedgerLine> findByEntryId(UUID entryId);

    List<LedgerLine> findByAccountNumberOrderByPostedAtDesc(String accountNumber);
}
