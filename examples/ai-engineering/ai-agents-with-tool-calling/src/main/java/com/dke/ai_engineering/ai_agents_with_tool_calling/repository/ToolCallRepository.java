package com.dke.ai_engineering.ai_agents_with_tool_calling.repository;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.ToolCall;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.ToolCallStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallRepository extends JpaRepository<ToolCall, Long> {

    List<ToolCall> findByRunIdOrderByStepNo(Long runId);

    Optional<ToolCall> findFirstByRunIdAndStatus(Long runId, ToolCallStatus status);
}
