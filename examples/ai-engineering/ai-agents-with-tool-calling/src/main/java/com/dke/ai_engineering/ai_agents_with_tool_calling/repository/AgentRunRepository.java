package com.dke.ai_engineering.ai_agents_with_tool_calling.repository;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
}
