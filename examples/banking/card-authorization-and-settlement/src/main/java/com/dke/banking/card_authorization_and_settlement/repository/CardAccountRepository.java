package com.dke.banking.card_authorization_and_settlement.repository;

import com.dke.banking.card_authorization_and_settlement.domain.CardAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardAccountRepository extends JpaRepository<CardAccount, String> {
}
