package com.dke.ai_engineering.llm_gateway_and_prompt_management.repository;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.TeamBudget;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TeamBudgetRepository extends JpaRepository<TeamBudget, Long> {

    Optional<TeamBudget> findByTeamId(String teamId);

    /** Row lock so two concurrent calls cannot both spend the last tokens. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from TeamBudget b where b.teamId = :teamId")
    Optional<TeamBudget> lockByTeamId(String teamId);
}
