package com.dke.ai_engineering.ai_agents_with_tool_calling.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.ai_engineering.ai_agents_with_tool_calling.integration.AuditTrailListener;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class AgentRunApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    AuditTrailListener audit;

    private String start(String goal) throws Exception {
        return mvc.perform(post("/api/agent-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"agent-user\",\"goal\":\"" + goal + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void readOnlyGoalCompletesAndPublishesEvents() throws Exception {
        mvc.perform(post("/api/agent-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"agent-user\",\"goal\":\"What is the status of CLM-1001 and book a callback\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.toolCalls.length()").value(2))
                .andExpect(jsonPath("$.finalAnswer").value(containsString("UNDER_REVIEW")));
        assertThat(audit.entries()).anyMatch(e -> e.startsWith("agent.run.finished") && e.contains("COMPLETED"));
    }

    @Test
    void paymentWaitsForApprovalAndRequesterCannotApprove() throws Exception {
        String body = start("Pay 250 USD on CLM-1002");
        String id = JsonPath.read(body, "$.id").toString();
        assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("AWAITING_APPROVAL");

        mvc.perform(post("/api/agent-runs/" + id + "/approval").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approver\":\"agent-user\",\"approved\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("cannot approve")));

        mvc.perform(post("/api/agent-runs/" + id + "/approval").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approver\":\"supervisor-1\",\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalAnswer").value(containsString("Payment of 250")));

        mvc.perform(post("/api/agent-runs/" + id + "/approval").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approver\":\"supervisor-1\",\"approved\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownToolIsDeniedAndStepLimitStopsRunawayLoops() throws Exception {
        mvc.perform(post("/api/agent-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"agent-user\",\"goal\":\"Delete CLM-1003\"}"))
                .andExpect(jsonPath("$.toolCalls[0].status").value("DENIED"));
        mvc.perform(post("/api/agent-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"agent-user\",\"goal\":\"Check CLM-1001 forever\"}"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.toolCalls.length()").value(4));
    }

    @Test
    void blankGoalIsRejected() throws Exception {
        mvc.perform(post("/api/agent-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"agent-user\",\"goal\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
