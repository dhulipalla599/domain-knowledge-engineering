package com.dke.banking.card_authorization_and_settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.banking.card_authorization_and_settlement.integration.CardEventListeners;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    CardEventListeners listeners;

    private static String auth(String ref, String card, String mcc, String amount, String currency) {
        return """
                {"networkReference":"%s","cardToken":"%s","merchantName":"Corner Grocer",
                 "mcc":"%s","amount":%s,"currency":"%s"}
                """.formatted(ref, card, mcc, amount, currency);
    }

    private String authorizeAndGetId(String body) throws Exception {
        String json = mvc.perform(post("/api/authorizations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void approveThenClearPostsToStatementAndPublishesEvents() throws Exception {
        String id = authorizeAndGetId(auth("API-HAPPY-1", "tok_4111_0001", "5411", "64.30", "USD"));
        mvc.perform(get("/api/authorizations/" + id))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.responseCode").value("00"));
        mvc.perform(post("/api/authorizations/" + id + "/clearing")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":64.30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"));
        assertThat(listeners.audit()).contains("card.authorization.approved:" + id, "card.transaction.cleared:" + id);
    }

    @Test
    void networkRetryWithSameReferenceDoesNotHoldTwice() throws Exception {
        String body = auth("API-RETRY-1", "tok_4111_0001", "5411", "10.00", "USD");
        String first = authorizeAndGetId(body);
        String second = authorizeAndGetId(body);
        assertThat(second).isEqualTo(first);
        mvc.perform(post("/api/authorizations").contentType(MediaType.APPLICATION_JSON)
                        .content(auth("API-RETRY-1", "tok_4111_0001", "5411", "11.00", "USD")))
                .andExpect(status().isConflict());
    }

    @Test
    void insufficientCreditIsADeclineNotAnError() throws Exception {
        mvc.perform(post("/api/authorizations").contentType(MediaType.APPLICATION_JSON)
                        .content(auth("API-NSF-1", "tok_4111_0002", "5411", "75.00", "USD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.responseCode").value("51"));
    }

    @Test
    void clearingAboveToleranceIsRejectedWithProblemDetail() throws Exception {
        String id = authorizeAndGetId(auth("API-OVER-1", "tok_4111_0001", "5411", "20.00", "USD"));
        mvc.perform(post("/api/authorizations/" + id + "/clearing")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":25.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("tolerance")));
    }

    @Test
    void wrongCurrencyAndReversedHoldAreRejected() throws Exception {
        mvc.perform(post("/api/authorizations").contentType(MediaType.APPLICATION_JSON)
                        .content(auth("API-FX-1", "tok_4111_0001", "5411", "20.00", "EUR")))
                .andExpect(status().isUnprocessableEntity());
        String id = authorizeAndGetId(auth("API-REV-1", "tok_4111_0001", "5411", "30.00", "USD"));
        mvc.perform(post("/api/authorizations/" + id + "/reversal")).andExpect(jsonPath("$.status").value("REVERSED"));
        mvc.perform(post("/api/authorizations/" + id + "/clearing")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":30.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void seededCardShowsOpenToBuy() throws Exception {
        mvc.perform(get("/api/cards/tok_4111_0003"))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.availableCredit").value(3000.00));
    }
}
