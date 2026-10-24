package com.dke.ai_engineering.llm_gateway_and_prompt_management.service;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptActivated;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptStatus;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptVersion;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.repository.PromptVersionRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptService {

    private final PromptVersionRepository prompts;
    private final ApplicationEventPublisher events;

    public PromptService(PromptVersionRepository prompts, ApplicationEventPublisher events) {
        this.prompts = prompts;
        this.events = events;
    }

    @Transactional
    public PromptVersion createDraft(String name, String template, String author) {
        return prompts.save(new PromptVersion(name, prompts.latestVersionNo(name) + 1, template, author));
    }

    /** Four-eyes rule: the author of a prompt can never approve it. */
    @Transactional
    public PromptVersion approve(Long id, String approver) {
        PromptVersion prompt = find(id);
        if (prompt.getCreatedBy().equalsIgnoreCase(approver)) {
            throw new BusinessRuleException(HttpStatus.FORBIDDEN, "Approval not allowed",
                    "The author of a prompt cannot approve it");
        }
        prompt.approve(approver);
        return prompt;
    }

    /** At most one ACTIVE version per prompt name: activating a version retires the previous one. */
    @Transactional
    public PromptVersion activate(Long id) {
        PromptVersion prompt = find(id);
        prompt.activate();
        prompts.findByNameAndStatus(prompt.getName(), PromptStatus.ACTIVE)
                .filter(current -> !current.getId().equals(prompt.getId()))
                .ifPresent(PromptVersion::retire);
        events.publishEvent(new PromptActivated(prompt.getId(), prompt.getName(), prompt.getVersionNo()));
        return prompt;
    }

    @Transactional(readOnly = true)
    public List<PromptVersion> history(String name) {
        return prompts.findByNameOrderByVersionNoDesc(name);
    }

    private PromptVersion find(Long id) {
        return prompts.findById(id).orElseThrow(() -> new BusinessRuleException(
                HttpStatus.NOT_FOUND, "Prompt not found", "No prompt version with id " + id));
    }
}
