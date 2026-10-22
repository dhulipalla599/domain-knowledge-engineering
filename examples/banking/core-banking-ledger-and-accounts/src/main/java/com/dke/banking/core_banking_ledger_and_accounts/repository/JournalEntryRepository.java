package com.dke.banking.core_banking_ledger_and_accounts.repository;

import com.dke.banking.core_banking_ledger_and_accounts.domain.JournalEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    boolean existsByReversalOf(UUID originalEntryId);
}
