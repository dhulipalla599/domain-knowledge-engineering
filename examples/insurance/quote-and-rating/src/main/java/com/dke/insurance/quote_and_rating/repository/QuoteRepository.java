package com.dke.insurance.quote_and_rating.repository;

import com.dke.insurance.quote_and_rating.domain.Quote;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
}
