package com.dke.ai_engineering.ai_agents_with_tool_calling.repository;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, String> {
}
