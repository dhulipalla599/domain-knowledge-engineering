package com.dke.banking.payments_processing_ach_wire_rtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.banking.payments_processing_ach_wire_rtp.integration.PaymentEventListeners;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    PaymentEventListeners listeners;

    private static String body(String rail, String amount) {
        return """
                {"fromAccount":"ACC-1001","rail":"%s","beneficiaryName":"Harbor Steel Supply",
                 "beneficiaryRouting":"021000021","beneficiaryAccount":"8800123456","amount":%s}
                """.formatted(rail, amount);
    }

    @Test
    void rtpHappyPathSettlesAndPublishesEvents() throws Exception {
        mvc.perform(post("/api/payments").header("Idempotency-Key", "api-happy-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("RTP", "250.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SETTLED"));
        assertThat(listeners.audit()).anyMatch(s -> s.startsWith("payments.submitted:"))
                .anyMatch(s -> s.startsWith("payments.settled:"));
    }

    @Test
    void achSettlesLaterAndCanBeReturned() throws Exception {
        String json = mvc.perform(post("/api/payments").header("Idempotency-Key", "api-ach-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("ACH", "100.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(post("/api/payments/" + id + "/settle")).andExpect(jsonPath("$.status").value("SETTLED"));
        mvc.perform(post("/api/payments/" + id + "/return")).andExpect(jsonPath("$.status").value("RETURNED"));
        mvc.perform(post("/api/payments/" + id + "/settle")).andExpect(status().isConflict());
    }

    @Test
    void amountOverRailLimitIsRejectedWithProblemDetail() throws Exception {
        mvc.perform(post("/api/payments").header("Idempotency-Key", "api-limit-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("RTP", "100000.01")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("limit")));
    }

    @Test
    void replayWithSameKeyReturnsSamePayment() throws Exception {
        String first = mvc.perform(post("/api/payments").header("Idempotency-Key", "api-replay-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("WIRE", "10.00")))
                .andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/payments").header("Idempotency-Key", "api-replay-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("WIRE", "10.00")))
                .andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);
        mvc.perform(post("/api/payments").header("Idempotency-Key", "api-replay-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("WIRE", "11.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void seededAccountIsReadable() throws Exception {
        mvc.perform(get("/api/accounts/ACC-1002")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Northwind Logistics"));
    }
}
