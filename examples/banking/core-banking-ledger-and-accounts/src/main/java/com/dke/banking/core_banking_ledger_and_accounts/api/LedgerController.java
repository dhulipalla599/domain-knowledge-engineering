package com.dke.banking.core_banking_ledger_and_accounts.api;

import com.dke.banking.core_banking_ledger_and_accounts.api.LedgerDtos.AccountResponse;
import com.dke.banking.core_banking_ledger_and_accounts.api.LedgerDtos.EntryResponse;
import com.dke.banking.core_banking_ledger_and_accounts.api.LedgerDtos.StatementLine;
import com.dke.banking.core_banking_ledger_and_accounts.api.LedgerDtos.StatusRequest;
import com.dke.banking.core_banking_ledger_and_accounts.api.LedgerDtos.TransferRequest;
import com.dke.banking.core_banking_ledger_and_accounts.service.LedgerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LedgerController {

    private final LedgerService ledger;

    public LedgerController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    EntryResponse transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                           @Valid @RequestBody TransferRequest request) {
        return EntryResponse.from(ledger.transfer(idempotencyKey, request.fromAccount(), request.toAccount(),
                request.amountMinor(), request.currency(), request.reference()));
    }

    @PostMapping("/transfers/{entryId}/reversal")
    @ResponseStatus(HttpStatus.CREATED)
    EntryResponse reverse(@PathVariable UUID entryId) {
        return EntryResponse.from(ledger.reverse(entryId));
    }

    @GetMapping("/accounts/{number}")
    AccountResponse account(@PathVariable String number) {
        return AccountResponse.from(ledger.account(number));
    }

    @GetMapping("/accounts/{number}/statement")
    List<StatementLine> statement(@PathVariable String number) {
        return ledger.statement(number).stream().map(StatementLine::from).toList();
    }

    @PostMapping("/accounts/{number}/status")
    AccountResponse changeStatus(@PathVariable String number, @Valid @RequestBody StatusRequest request) {
        return AccountResponse.from(ledger.changeStatus(number, request.status()));
    }
}
