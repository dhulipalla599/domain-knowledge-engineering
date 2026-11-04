package com.dke.insurance.quote_and_rating.repository;

import com.dke.insurance.quote_and_rating.domain.BaseRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseRateRepository extends JpaRepository<BaseRate, String> {
}
