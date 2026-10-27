package com.dke.insurance.underwriting.api;

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
class UnderwritingApiTest {

    @Autowired
    MockMvc mvc;

    private static final String CLEAN = """
            {"applicantName":"Jo Park","sumInsured":250000,"yearBuilt":2012,"floodZone":false}""";
    private static final String BIG_FLOOD = """
            {"applicantName":"Lena Okafor","sumInsured":900000,"yearBuilt":1938,"floodZone":true}""";

    @Test
    void cleanApplicationIsApprovedAndReplayIsIdempotent() throws Exception {
        mvc.perform(post("/api/applications").header("Idempotency-Key", "t-clean")
                        .contentType(MediaType.APPLICATION_JSON).content(CLEAN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/applications").header("Idempotency-Key", "t-clean")
                        .contentType(MediaType.APPLICATION_JSON).content(CLEAN))
                .andExpect(status().isOk());
    }

    @Test
    void juniorCannotApproveAboveAuthorityButSeniorCan() throws Exception {
        var body = mvc.perform(post("/api/applications").header("Idempotency-Key", "t-big")
                        .contentType(MediaType.APPLICATION_JSON).content(BIG_FLOOD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REFERRED"))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/applications/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"underwriterId\":\"uw-junior\",\"approve\":true,\"reason\":\"ok\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/applications/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"underwriterId\":\"uw-senior\",\"approve\":true,\"reason\":\"flood cover added\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/applications/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"underwriterId\":\"uw-senior\",\"approve\":false,\"reason\":\"again\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRequestIsRejected() throws Exception {
        mvc.perform(post("/api/applications").header("Idempotency-Key", "t-bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantName\":\"\",\"sumInsured\":-5,\"yearBuilt\":2000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void seededReferralQueueIsVisible() throws Exception {
        mvc.perform(get("/api/applications/referrals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicantName").value("Priya Nair"));
    }
}
