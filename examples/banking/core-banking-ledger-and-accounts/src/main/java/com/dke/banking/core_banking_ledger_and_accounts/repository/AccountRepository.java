package com.dke.banking.core_banking_ledger_and_accounts.repository;

import com.dke.banking.core_banking_ledger_and_accounts.domain.Account;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByNumber(String number);

    /** SELECT ... FOR UPDATE: serialises concurrent postings against the same account. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.number = :number")
    Optional<Account> findByNumberForUpdate(String number);
}
