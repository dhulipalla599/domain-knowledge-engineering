package com.dke.data_engineering.data_quality_and_validation.repository;

import com.dke.data_engineering.data_quality_and_validation.domain.RuleViolation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleViolationRepository extends JpaRepository<RuleViolation, Long> {

    List<RuleViolation> findByRunIdOrderByLineNumberAsc(Long runId);
}
