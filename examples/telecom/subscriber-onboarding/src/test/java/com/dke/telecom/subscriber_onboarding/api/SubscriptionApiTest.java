package com.dke.telecom.subscriber_onboarding.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionApiTest {

    @Autowired
    MockMvc mvc;

    private static String body(String docNumber, String dob, String iccid) {
        return """
                {"fullName":"Test User","dateOfBirth":"%s","idDocumentNumber":"%s",
                 "plan":"PREPAID_BASIC","iccid":"%s"}""".formatted(dob, docNumber, iccid);
    }

    @Test
    void happyPathActivatesSubscriptionAndIsIdempotent() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "api-happy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("ID-880001", "1992-06-01", "8944000000000000002")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.msisdn").value("+15550100002"));
        }
    }

    @Test
    void failedIdentityCheckReturns422ProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "api-fake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("FAKE-123", "1992-06-01", "8944000000000000003")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Identity verification failed"));
    }

    @Test
    void assignedSimReturns409() throws Exception {
        mvc.perform(post("/api/v1/subscriptions").header("Idempotency-Key", "api-taken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ID-880002", "1992-06-01", "8944000000000000099")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("SIM already assigned"));
    }

    @Test
    void seededSubscriptionCanBeRead() throws Exception {
        mvc.perform(get("/api/v1/subscriptions/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
