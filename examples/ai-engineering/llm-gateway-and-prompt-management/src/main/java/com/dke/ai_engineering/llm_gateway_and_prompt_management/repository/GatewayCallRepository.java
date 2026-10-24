package com.dke.ai_engineering.llm_gateway_and_prompt_management.repository;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.GatewayCall;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatewayCallRepository extends JpaRepository<GatewayCall, Long> {

    Optional<GatewayCall> findByTeamIdAndIdempotencyKey(String teamId, String idempotencyKey);
}
