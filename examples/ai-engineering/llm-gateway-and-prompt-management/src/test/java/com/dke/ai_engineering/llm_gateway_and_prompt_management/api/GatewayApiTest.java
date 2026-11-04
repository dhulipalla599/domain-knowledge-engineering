package com.dke.ai_engineering.llm_gateway_and_prompt_management.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.integration.AuditTrailListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuditTrailListener audit;

    private ResultActions complete(String team, String notes, String key) throws Exception {
        var request = post("/api/gateway/completions").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"teamId":"%s","promptName":"claims-summary","maxTokens":50,"variables":{"notes":"%s"}}
                        """.formatted(team, notes));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mvc.perform(request);
    }

    @Test
    void happyPathRedactsPiiAndPublishesEvent() throws Exception {
        complete("claims-team", "Water damage, contact jo@example.com", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider", is("primary-stub")))
                .andExpect(jsonPath("$.promptVersion", is(1)))
                .andExpect(jsonPath("$.redactions", is(1)))
                .andExpect(jsonPath("$.output", startsWith("[primary-stub]")));
        org.assertj.core.api.Assertions.assertThat(audit.tokensFor("claims-team")).isPositive();
    }

    @Test
    void failsOverToSecondProviderOnOutage() throws Exception {
        complete("claims-team", "[simulate-outage] storm claim", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider", is("fallback-stub")));
    }

    @Test
    void repeatedIdempotencyKeyIsNotChargedTwice() throws Exception {
        complete("claims-team", "Same call", "key-1").andExpect(status().isOk());
        String used = mvc.perform(get("/api/budgets/claims-team")).andReturn().getResponse().getContentAsString();
        complete("claims-team", "Same call", "key-1").andExpect(status().isOk());
        String after = mvc.perform(get("/api/budgets/claims-team")).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(used);
    }

    @Test
    void rejectsCallWhenBudgetWouldBeExceeded() throws Exception {
        complete("marketing-team", "Campaign draft", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title", is("Token budget exceeded")));
    }

    @Test
    void rejectsMissingVariable() throws Exception {
        mvc.perform(post("/api/gateway/completions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId":"claims-team","promptName":"support-faq","variables":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("question")));
    }

    @Test
    void promptGoesDraftApprovedActiveWithFourEyes() throws Exception {
        String created = mvc.perform(post("/api/prompts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"lifecycle-demo","template":"Hi {{who}}","author":"ann"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/prompts/" + id + "/activate")).andExpect(status().isConflict());
        mvc.perform(post("/api/prompts/" + id + "/approve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approver\":\"ann\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/prompts/" + id + "/approve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approver\":\"bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
        mvc.perform(post("/api/prompts/" + id + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}
