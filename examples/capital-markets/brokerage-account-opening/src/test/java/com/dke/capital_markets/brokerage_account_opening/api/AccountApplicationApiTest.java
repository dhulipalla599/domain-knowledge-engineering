package com.dke.capital_markets.brokerage_account_opening.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.capital_markets.brokerage_account_opening.integration.AuditTrailListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountApplicationApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuditTrailListener audit;

    private static String body(String name, String dob, String taxId, String type, String netWorth) {
        return """
                {"applicantName":"%s","dateOfBirth":"%s","taxIdLast4":"%s","accountType":"%s","liquidNetWorth":%s}
                """.formatted(name, dob, taxId, type, netWorth);
    }

    @Test
    void happyPathApprovesThenActivatesAndPublishesEvents() throws Exception {
        var created = mvc.perform(post("/api/v1/applications")
                        .header("Idempotency-Key", "test-happy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Priya Raman", "1991-06-20", "9012", "MARGIN", "60000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/v1/applications/" + id + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountNumber").exists());

        assertThat(audit.entries()).anyMatch(e -> e.startsWith("brokerage.application.decided") && e.contains(id));
        assertThat(audit.entries()).anyMatch(e -> e.startsWith("brokerage.account.activated") && e.contains(id));
    }

    @Test
    void replayedIdempotencyKeyReturnsSameApplication() throws Exception {
        var payload = body("Lena Fischer", "1985-03-03", "5566", "CASH", "1000");
        mvc.perform(post("/api/v1/applications").header("Idempotency-Key", "test-replay")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/applications").header("Idempotency-Key", "test-replay")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
    }

    @Test
    void marginAccountBelowNetWorthIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/v1/applications")
                        .header("Idempotency-Key", "test-margin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Tom Baker", "1980-01-01", "1111", "MARGIN", "5000")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Margin")));
    }

    @Test
    void possibleSanctionsMatchGoesToReviewAndCannotBeActivated() throws Exception {
        var created = mvc.perform(post("/api/v1/applications")
                        .header("Idempotency-Key", "test-sanctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sam Blocklisted", "1979-09-09", "2222", "CASH", "9000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/v1/applications/" + id + "/activate")).andExpect(status().isConflict());
    }

    @Test
    void seededApplicationsAreListed() throws Exception {
        mvc.perform(get("/api/v1/applications").param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].applicantName").value(org.hamcrest.Matchers.hasItem("Ivan Blocklisted-Example")));
    }
}
