package com.dke.banking.customer_onboarding_and_kyc.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Drives the API over HTTP (MockMvc) with the real database, listeners and policies. */
@SpringBootTest
@AutoConfigureMockMvc
class OnboardingApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private JsonNode submit(String key, String name, String dob, String nationality, String occupation,
                            int expectedStatus) throws Exception {
        String body = """
                {"legalName":"%s","dateOfBirth":"%s","nationality":"%s","occupation":"%s"}
                """.formatted(name, dob, nationality, occupation);
        String response = mvc.perform(post("/api/v1/applications")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    @Test
    void lowRiskApplicantIsApprovedAndGetsARestrictedAccount() throws Exception {
        JsonNode first = submit("api-test-1", "Priya Raman", "1991-04-12", "IN", "Software engineer", 202);
        assertThat(first.get("status").asText()).isEqualTo("APPROVED");
        assertThat(first.get("accountNumber").asText()).startsWith("NWB");

        // A retry with the same Idempotency-Key returns the same application, not a second one.
        JsonNode retry = submit("api-test-1", "Priya Raman", "1991-04-12", "IN", "Software engineer", 202);
        assertThat(retry.get("applicationId").asText()).isEqualTo(first.get("applicationId").asText());
    }

    @Test
    void minorIsRejectedWithA422Problem() throws Exception {
        JsonNode problem = submit("api-test-2", "Sam Young", "2015-06-01", "GB", "Student", 422);
        assertThat(problem.get("code").asText()).isEqualTo("UNDER_MINIMUM_AGE");
    }

    @Test
    void possibleSanctionsMatchWaitsForAnalystAndIsNotDisclosed() throws Exception {
        JsonNode app = submit("api-test-3", "Viktor Petrov", "1988-02-20", "GB", "Teacher", 202);
        String id = app.get("applicationId").asText();
        assertThat(app.get("status").asText()).isEqualTo("IN_PROGRESS");   // no "review" wording

        String queue = mvc.perform(get("/api/v1/review-cases"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(queue).contains(id).contains("POSSIBLE_MATCH");

        mvc.perform(post("/api/v1/review-cases/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cleared\":true,\"reason\":\"Different date of birth from the listed person\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/v1/applications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.accountNumber").isNotEmpty());
    }

    @Test
    void confirmedSanctionsMatchIsDeclined() throws Exception {
        JsonNode app = submit("api-test-4", "Ivan Drago", "1962-07-04", "GB", "Boxer", 202);
        assertThat(app.get("status").asText()).isEqualTo("DECLINED");
        assertThat(app.get("accountNumber").isNull()).isTrue();
    }

    @Test
    void missingIdempotencyKeyIsABadRequest() throws Exception {
        mvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legalName\":\"A B\",\"dateOfBirth\":\"1990-01-01\","
                                + "\"nationality\":\"GB\",\"occupation\":\"Chef\"}"))
                .andExpect(status().isBadRequest());
    }
}
