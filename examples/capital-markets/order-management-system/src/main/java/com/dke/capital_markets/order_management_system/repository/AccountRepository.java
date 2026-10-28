package com.dke.capital_markets.order_management_system.repository;

import com.dke.capital_markets.order_management_system.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
