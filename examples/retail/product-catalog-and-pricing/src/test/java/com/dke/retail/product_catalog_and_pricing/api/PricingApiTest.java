package com.dke.retail.product_catalog_and_pricing.api;

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
class PricingApiTest {

    @Autowired
    MockMvc mvc;

    private String body(String sku, String price) {
        return "{\"sku\":\"" + sku + "\",\"newPrice\":" + price + ",\"reason\":\"test\"}";
    }

    @Test
    void smallChangeIsAppliedImmediately() throws Exception {
        mvc.perform(post("/api/price-changes").contentType(MediaType.APPLICATION_JSON).content(body("SKU-1002", "18.90")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"));
        mvc.perform(get("/api/products/SKU-1002"))
                .andExpect(jsonPath("$.price").value(18.90));
    }

    @Test
    void largeChangeWaitsForApprovalThenApplies() throws Exception {
        mvc.perform(post("/api/price-changes").contentType(MediaType.APPLICATION_JSON).content(body("SKU-1003", "9.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        mvc.perform(post("/api/price-changes").contentType(MediaType.APPLICATION_JSON).content(body("SKU-1003", "9.50")))
                .andExpect(status().isConflict());
    }

    @Test
    void priceBelowMarginFloorReturns422() throws Exception {
        mvc.perform(post("/api/price-changes").contentType(MediaType.APPLICATION_JSON).content(body("SKU-1004", "10.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("margin floor")));
    }
}
