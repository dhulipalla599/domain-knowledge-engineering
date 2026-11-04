package com.dke.banking.payments_processing_ach_wire_rtp.api;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.Account;
import java.math.BigDecimal;

public record AccountResponse(String accountNumber, String ownerName, BigDecimal balance,
                              BigDecimal held, BigDecimal available) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getAccountNumber(), a.getOwnerName(), a.getBalance(), a.getHeld(), a.available());
    }
}
