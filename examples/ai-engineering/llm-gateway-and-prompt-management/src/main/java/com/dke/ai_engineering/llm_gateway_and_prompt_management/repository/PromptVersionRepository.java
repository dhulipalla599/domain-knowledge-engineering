package com.dke.ai_engineering.llm_gateway_and_prompt_management.repository;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptStatus;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    Optional<PromptVersion> findByNameAndStatus(String name, PromptStatus status);

    List<PromptVersion> findByNameOrderByVersionNoDesc(String name);

    @Query("select coalesce(max(p.versionNo), 0) from PromptVersion p where p.name = :name")
    int latestVersionNo(String name);
}
