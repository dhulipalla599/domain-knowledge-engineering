package com.dke.retail.store_replenishment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.retail.store_replenishment.integration.ReplenishmentEventListeners;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReplenishmentApiTest {

    @Autowired MockMvc mvc;
    @Autowired ReplenishmentEventListeners listeners;

    private static final String RUN = "{\"storeId\":\"S-104\",\"runDate\":\"2026-11-27\"}";

    @Test
    void runReleaseAndReceiveHappyPathIsIdempotent() throws Exception {
        String body = mvc.perform(post("/api/replenishment-runs").contentType(MediaType.APPLICATION_JSON).content(RUN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created", is(2)))
                .andExpect(jsonPath("$.orders[0].sku", is("EGGS-12")))
                .andExpect(jsonPath("$.orders[0].quantity", is(30)))
                .andExpect(jsonPath("$.orders[1].sku", is("MILK-2L")))
                .andExpect(jsonPath("$.orders[1].quantity", is(48)))
                .andReturn().getResponse().getContentAsString();
        String milkOrder = JsonPath.read(body, "$.orders[1].id");

        // same store and date again: 200, same orders, nothing new
        mvc.perform(post("/api/replenishment-runs").contentType(MediaType.APPLICATION_JSON).content(RUN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(0)))
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.orders[1].id", is(milkOrder)));

        mvc.perform(post("/api/orders/" + milkOrder + "/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RELEASED")));
        mvc.perform(get("/api/stores/S-104/skus/MILK-2L")).andExpect(jsonPath("$.inventoryPosition", is(66)));

        mvc.perform(post("/api/orders/" + milkOrder + "/receive").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receivedQuantity\":44}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RECEIVED")));
        mvc.perform(get("/api/stores/S-104/skus/MILK-2L"))
                .andExpect(jsonPath("$.onHand", is(62)))
                .andExpect(jsonPath("$.onOrder", is(0)));
        mvc.perform(post("/api/orders/" + milkOrder + "/cancel")).andExpect(status().isConflict());

        assertThat(listeners.publishedTopics()).contains(
                "replenishment.order-proposed", "replenishment.order-released", "replenishment.order-received");
    }

    @Test
    void manualOrderRulesAreEnforced() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"S-207\",\"sku\":\"MILK-2L\",\"quantity\":20}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", is("Quantity 20 of MILK-2L is not a multiple of the case pack 12")));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"S-207\",\"sku\":\"MILK-2L\",\"quantity\":36}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", is(
                        "Ordering 36 of MILK-2L would raise the inventory position from 40 to 76, above shelf capacity 72")));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"S-207\",\"sku\":\"MILK-2L\",\"quantity\":24}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source", is("MANUAL")))
                .andExpect(jsonPath("$.status", is("PROPOSED")));
    }

    @Test
    void unknownStoreAndInvalidRequest() throws Exception {
        mvc.perform(post("/api/replenishment-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"S-999\",\"runDate\":\"2026-11-27\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/replenishment-runs").contentType(MediaType.APPLICATION_JSON).content("{\"storeId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
