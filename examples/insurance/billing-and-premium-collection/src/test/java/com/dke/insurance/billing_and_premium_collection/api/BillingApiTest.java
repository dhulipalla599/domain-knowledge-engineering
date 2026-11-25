package com.dke.insurance.billing_and_premium_collection.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class BillingApiTest {

    @Autowired
    MockMvc mvc;

    private ResultActions pay(long installmentId, String key, String amount, String token) throws Exception {
        return mvc.perform(post("/api/installments/" + installmentId + "/payments")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + ",\"paymentToken\":\"" + token + "\"}"));
    }

    @Test
    void collectsInstallmentInTwoPaymentsAndReplaysRetry() throws Exception {
        pay(1, "key-happy-1", "40.00", "tok_visa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installmentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.outstanding").value(60.00))
                .andExpect(jsonPath("$.replayed").value(false));

        pay(1, "key-happy-2", "60.00", "tok_visa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installmentStatus").value("PAID"));

        // Client timed out and retried the second call: no second charge, same payment returned.
        pay(1, "key-happy-2", "60.00", "tok_visa")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.installmentStatus").value("PAID"));

        mvc.perform(get("/api/installments/1"))
                .andExpect(jsonPath("$.amountPaid").value(100.00));
    }

    @Test
    void rejectsOverpayment() throws Exception {
        pay(2, "key-over-1", "100.00", "tok_visa")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("exceeds outstanding premium 85.50")));
    }

    @Test
    void rejectsPaymentOnSettledInstallmentAndDeclinedCard() throws Exception {
        pay(3, "key-paid-1", "1.00", "tok_visa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("is PAID")));
        pay(4, "key-decline-1", "64.20", "tok_insufficient_funds")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.detail").value("Payment declined: insufficient_funds"));
    }

    @Test
    void requiresIdempotencyKey() throws Exception {
        mvc.perform(post("/api/installments/4/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"paymentToken\":\"tok_visa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Header Idempotency-Key is required"));
    }
}
