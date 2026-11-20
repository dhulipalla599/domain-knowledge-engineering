package com.dke.healthcare.insurance_eligibility_verification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EligibilityApiTest {

    @Autowired
    MockMvc mvc;

    private String body(String coverageId, LocalDate serviceDate) {
        return "{\"coverageId\":\"%s\",\"serviceDate\":\"%s\",\"serviceTypeCode\":\"30\"}".formatted(coverageId, serviceDate);
    }

    @Test
    void verifiesActiveCoverageThenReusesTheFreshAnswer() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON).content(body("cov-1001", today)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.copayAmount").value(30.00))
                .andExpect(jsonPath("$.reused").value(false));

        mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON).content(body("cov-1001", today)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reused").value(true));
    }

    @Test
    void reportsInactiveAndRejectedAnswersAsCompletedChecks() throws Exception {
        mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON).content(body("cov-1002", LocalDate.now(ZoneOffset.UTC))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON).content(body("cov-1003", LocalDate.now(ZoneOffset.UTC))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReasonCode").value("72"));
    }

    @Test
    void rejectsRuleViolations() throws Exception {
        mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cov-1001", LocalDate.now(ZoneOffset.UTC).plusDays(90))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());
        mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON).content(body("cov-1005", LocalDate.now(ZoneOffset.UTC))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void retriesPayerErrorsButNotCompletedChecks() throws Exception {
        String json = mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cov-1004", LocalDate.now(ZoneOffset.UTC))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAYER_ERROR"))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(post("/api/eligibility-checks/" + id + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYER_ERROR"));

        String active = mvc.perform(post("/api/eligibility-checks").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cov-1001", LocalDate.now(ZoneOffset.UTC).minusDays(3))))
                .andReturn().getResponse().getContentAsString();
        String activeId = active.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(post("/api/eligibility-checks/" + activeId + "/retry"))
                .andExpect(status().isConflict());
    }
}
