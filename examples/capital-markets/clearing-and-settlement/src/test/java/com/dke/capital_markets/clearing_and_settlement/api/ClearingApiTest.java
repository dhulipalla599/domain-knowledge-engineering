package com.dke.capital_markets.clearing_and_settlement.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Runs against the seeded data and the fixed demo clock (Wed 25 Nov 2026, 09:00 New York). */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClearingApiTest {

    @Autowired
    MockMvc mvc;

    private static String trade(String id, String buyer, String seller, String date) {
        return "{\"tradeId\":\"" + id + "\",\"buyerAccount\":\"" + buyer + "\",\"sellerAccount\":\"" + seller + "\","
                + "\"symbol\":\"ACME\",\"quantity\":200,\"price\":45.30,\"tradeDate\":\"" + date + "\"}";
    }

    @Test
    void captureComputesTPlusOneAcrossHolidayAndReplayIsIdempotent() throws Exception {
        mvc.perform(post("/api/trades").contentType(MediaType.APPLICATION_JSON)
                        .content(trade("T-2001", "REDCLIFF", "BLUEHARBOR", "2026-11-25")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_AFFIRMATION"))
                .andExpect(jsonPath("$.settlementDate").value("2026-11-27"));
        mvc.perform(post("/api/trades").contentType(MediaType.APPLICATION_JSON)
                        .content(trade("T-2001", "REDCLIFF", "BLUEHARBOR", "2026-11-25")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/trades/T-2001/affirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AFFIRMED"));
    }

    @Test
    void settlementRunSettlesDeliverableTradesAndFailsShortOnes() throws Exception {
        mvc.perform(post("/api/settlement-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settlementDate\":\"2026-11-25\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled[0]").value("T-1001"))
                .andExpect(jsonPath("$.failed[0]").value("T-1002"));
        mvc.perform(get("/api/trades/T-1002"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failReason").value("INSUFFICIENT_SECURITIES"));
        mvc.perform(get("/api/accounts/NORTHWIND/positions"))
                .andExpect(jsonPath("$[0].asset").value("ACME"))
                .andExpect(jsonPath("$[0].quantity").value(1400))
                .andExpect(jsonPath("$[1].asset").value("USD"))
                .andExpect(jsonPath("$[1].quantity").value(204900.00));
        mvc.perform(post("/api/trades/T-1001/cancel")).andExpect(status().isConflict());
    }

    @Test
    void lateAffirmationIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/trades/T-1003/affirm"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("AFFIRMATION_CUTOFF"));
    }

    @Test
    void weekendTradeDateIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/trades").contentType(MediaType.APPLICATION_JSON)
                        .content(trade("T-2002", "REDCLIFF", "BLUEHARBOR", "2026-11-22")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("TRADE_DATE"));
    }

    @Test
    void netObligationsAreNettedPerAccountAndAsset() throws Exception {
        mvc.perform(get("/api/obligations").param("settlementDate", "2026-11-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }
}
