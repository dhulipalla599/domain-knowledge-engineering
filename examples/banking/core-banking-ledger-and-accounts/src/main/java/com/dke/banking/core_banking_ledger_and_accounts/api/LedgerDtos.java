package com.dke.banking.core_banking_ledger_and_accounts.api;

import com.dke.banking.core_banking_ledger_and_accounts.domain.Account;
import com.dke.banking.core_banking_ledger_and_accounts.domain.AccountStatus;
import com.dke.banking.core_banking_ledger_and_accounts.domain.Direction;
import com.dke.banking.core_banking_ledger_and_accounts.domain.EntryStatus;
import com.dke.banking.core_banking_ledger_and_accounts.domain.JournalEntry;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerLine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class LedgerDtos {

    private LedgerDtos() {
    }

    public record TransferRequest(@NotBlank String fromAccount, @NotBlank String toAccount,
                                  @Positive long amountMinor, @NotBlank @Size(min = 3, max = 3) String currency,
                                  @Size(max = 140) String reference) {
    }

    public record StatusRequest(@NotNull AccountStatus status) {
    }

    public record EntryResponse(UUID entryId, EntryStatus status, long amountMinor, String currency, String reference,
                                UUID reversalOf, Instant createdAt) {
        static EntryResponse from(JournalEntry e) {
            return new EntryResponse(e.getId(), e.getStatus(), e.getAmountMinor(), e.getCurrency(), e.getReference(),
                    e.getReversalOf(), e.getCreatedAt());
        }
    }

    public record AccountResponse(String number, String ownerName, String currency, long balanceMinor,
                                  long availableMinor, AccountStatus status) {
        static AccountResponse from(Account a) {
            return new AccountResponse(a.getNumber(), a.getOwnerName(), a.getCurrency(), a.getBalanceMinor(),
                    a.availableMinor(), a.getStatus());
        }
    }

    public record StatementLine(UUID entryId, Direction direction, long amountMinor, Instant postedAt) {
        static StatementLine from(LedgerLine l) {
            return new StatementLine(l.getEntryId(), l.getDirection(), l.getAmountMinor(), l.getPostedAt());
        }
    }
}
