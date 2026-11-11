package com.dke.capital_markets.trade_execution_and_smart_order_routing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.ExecutionEventListeners;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ExecutionEventListeners listeners;

    private static String order(String clientId, String symbol, String side, long qty, String price) {
        return """
                {"clientOrderId":"%s","accountId":"ACC-7781","symbol":"%s","side":"%s","quantity":%d,"limitPrice":%s}
                """.formatted(clientId, symbol, side, qty, price);
    }

    @Test
    void marketableBuyIsSplitAcrossVenuesAndFilled() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order("T-100", "ACME", "BUY", 800, "100.04")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.filledQuantity").value(800))
                .andExpect(jsonPath("$.averagePrice").value(100.0238))
                .andExpect(jsonPath("$.childOrders.length()").value(3))
                .andExpect(jsonPath("$.childOrders[0].venue").value("BATS"));
        assertThat(listeners.auditTrail()).anyMatch(s -> s.startsWith("execution.fill-reported | ARCX 300"));
    }

    @Test
    void partialFillRestsThenCanBeCancelledButNotTwice() throws Exception {
        String body = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order("T-101", "ACME", "BUY", 700, "100.02")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIALLY_FILLED"))
                .andExpect(jsonPath("$.leavesQuantity").value(200))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll("^\\{\"id\":(\\d+).*", "$1");
        mvc.perform(post("/api/orders/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.childOrders[2].status").value("CANCELLED"));
        mvc.perform(post("/api/orders/" + id + "/cancel")).andExpect(status().isConflict());
    }

    @Test
    void fatFingerPriceIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order("T-102", "ACME", "BUY", 100, "120.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("PRICE_COLLAR"));
    }

    @Test
    void haltedSymbolIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order("T-103", "HLTD", "SELL", 100, "12.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("SYMBOL_HALTED"));
    }

    @Test
    void reusedClientOrderIdIsRejectedWith409() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order("SEED-0001", "GLBX", "BUY", 10, "41.12")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("DUPLICATE_CLIENT_ORDER_ID"));
    }

    @Test
    void seededOrderIsReadable() throws Exception {
        mvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientOrderId").value("SEED-0001"))
                .andExpect(jsonPath("$.childOrders.length()").value(2));
    }
}
