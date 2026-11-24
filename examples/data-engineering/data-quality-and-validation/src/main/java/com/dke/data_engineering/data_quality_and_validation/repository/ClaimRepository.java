package com.dke.data_engineering.data_quality_and_validation.repository;

import com.dke.data_engineering.data_quality_and_validation.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, String> { }
