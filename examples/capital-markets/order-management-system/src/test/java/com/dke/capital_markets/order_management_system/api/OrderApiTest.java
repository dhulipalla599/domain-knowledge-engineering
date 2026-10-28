package com.dke.capital_markets.order_management_system.api;

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
class OrderApiTest {

    @Autowired
    MockMvc mvc;

    private static String order(String account, String clientId, long qty, String price) {
        return "{\"accountId\":\"" + account + "\",\"clientOrderId\":\"" + clientId + "\",\"symbol\":\"ACME\","
                + "\"side\":\"BUY\",\"quantity\":" + qty + ",\"limitPrice\":" + price + "}";
    }

    @Test
    void happyPathPlaceFillAndReplay() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order("ACC-1001", "t-1", 100, "25.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order("ACC-1001", "t-1", 100, "25.50")))
                .andExpect(status().isOk());
    }

    @Test
    void executionsMoveOrderToFilled() throws Exception {
        String body = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order("ACC-1001", "t-2", 10, "20.00")))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(post("/api/orders/" + id + "/executions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10,\"price\":19.95}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FILLED"));
        mvc.perform(post("/api/orders/" + id + "/cancel")).andExpect(status().isConflict());
        mvc.perform(get("/api/orders/" + id)).andExpect(jsonPath("$.filledQuantity").value(10));
    }

    @Test
    void insufficientBuyingPowerIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order("ACC-1002", "t-3", 1000, "50.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("BUYING_POWER"));
    }

    @Test
    void subPennyPriceIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order("ACC-1001", "t-4", 1, "10.005")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("TICK_SIZE"));
    }
}
