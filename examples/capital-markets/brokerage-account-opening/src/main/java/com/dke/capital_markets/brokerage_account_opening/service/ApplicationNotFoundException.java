package com.dke.capital_markets.brokerage_account_opening.service;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("Application " + id + " not found");
    }
}
