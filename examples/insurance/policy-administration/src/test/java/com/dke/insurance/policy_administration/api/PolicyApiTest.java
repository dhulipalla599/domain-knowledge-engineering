package com.dke.insurance.policy_administration.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyApiTest {

    @Autowired
    MockMvc mvc;

    private static final String ISSUE = """
            {"quoteId":"%s","holderName":"Ana Silva","sumInsured":300000,"rate":0.003,"inception":"2026-01-01"}""";

    @Test
    void issuesEndorsesAndCancelsOverHttp() throws Exception {
        var body = mvc.perform(post("/api/policies").contentType(MediaType.APPLICATION_JSON).content(ISSUE.formatted("Q-HAPPY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.annualPremium").value(900.00))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/policies/" + id + "/endorsements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newSumInsured\":400000,\"effectiveDate\":\"2026-07-01\",\"reason\":\"Extension\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualPremium").value(1200.00))
                .andExpect(jsonPath("$.endorsements[0].premiumDelta").value(151.23));

        mvc.perform(post("/api/policies/" + id + "/cancellation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effectiveDate\":\"2026-07-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundDue").value(604.93));

        mvc.perform(post("/api/policies/" + id + "/cancellation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effectiveDate\":\"2026-08-01\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsSecondPolicyForSameQuote() throws Exception {
        mvc.perform(post("/api/policies").contentType(MediaType.APPLICATION_JSON).content(ISSUE.formatted("Q-DUP")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/policies").contentType(MediaType.APPLICATION_JSON).content(ISSUE.formatted("Q-DUP")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already has a policy")));
    }

    @Test
    void rejectsEndorsementOutsideTermAndDeclinedPayment() throws Exception {
        mvc.perform(post("/api/policies/1/endorsements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newSumInsured\":500000,\"effectiveDate\":\"2028-01-01\",\"reason\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("outside the policy term")));
        mvc.perform(post("/api/policies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quoteId\":\"Q-BIG\",\"holderName\":\"Big Co\",\"sumInsured\":30000000,\"rate\":0.01,\"inception\":\"2026-01-01\"}"))
                .andExpect(status().isPaymentRequired());
    }
}
