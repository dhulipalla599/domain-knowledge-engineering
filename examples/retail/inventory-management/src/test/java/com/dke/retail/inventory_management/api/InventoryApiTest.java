package com.dke.retail.inventory_management.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.retail.inventory_management.integration.StubReplenishmentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiTest {

    @Autowired MockMvc mvc;
    @Autowired StubReplenishmentGateway replenishment;

    private String body(String order, String sku, int qty) {
        return "{\"orderRef\":\"%s\",\"sku\":\"%s\",\"quantity\":%d}".formatted(order, sku, qty);
    }

    @Test
    void reserveThenConfirmReducesOnHandAndRetryIsIdempotent() throws Exception {
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body("ORD-A", "SKU-1002", 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")));

        // same orderRef + sku again: 200 and no second hold
        String id = mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body("ORD-A", "SKU-1002", 5)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/stock/SKU-1002")).andExpect(jsonPath("$.reserved", is(5)));

        mvc.perform(post("/api/reservations/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
        mvc.perform(get("/api/stock/SKU-1002"))
                .andExpect(jsonPath("$.onHand", is(35)))
                .andExpect(jsonPath("$.reserved", is(0)));
        mvc.perform(post("/api/reservations/" + id + "/release")).andExpect(status().isConflict());
    }

    @Test
    void overReservationIsRejectedWith409() throws Exception {
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body("ORD-B", "SKU-1003", 9)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", is("Cannot reserve 9 of SKU-1003: only 3 available")));
    }

    @Test
    void lowStockTriggersReorder() throws Exception {
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body("ORD-C", "SKU-1004", 20)))
                .andExpect(status().isCreated());
        assertThat(replenishment.requests()).anyMatch(r -> r.startsWith("PO-SKU-1004"));
    }

    @Test
    void invalidQuantityAndUnknownSku() throws Exception {
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body("ORD-D", "SKU-1001", 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body("ORD-E", "NOPE", 1)))
                .andExpect(status().isNotFound());
    }
}
