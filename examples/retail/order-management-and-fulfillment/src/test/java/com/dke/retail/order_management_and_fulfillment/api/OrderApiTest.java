package com.dke.retail.order_management_and_fulfillment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.retail.order_management_and_fulfillment.integration.OrderEventListeners;
import com.jayway.jsonpath.JsonPath;
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
    @Autowired
    OrderEventListeners listeners;

    private static final String KETTLE_AND_SKILLET = """
            {"customerId":"CUST-4410","shipToRegion":"EAST","paymentToken":"tok_visa",
             "lines":[{"sku":"KTL-200","quantity":2},{"sku":"FRY-28","quantity":1}]}
            """;

    @Test
    void orderIsAllocatedShippedAndRetriesAreIdempotent() throws Exception {
        String body = mvc.perform(post("/api/orders").header("Idempotency-Key", "chk-api-1")
                        .contentType(MediaType.APPLICATION_JSON).content(KETTLE_AND_SKILLET))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ALLOCATED"))
                .andExpect(jsonPath("$.totalAmount").value(133.98))
                .andExpect(jsonPath("$.lines[0].fulfillmentNode").value("STORE-114"))
                .andExpect(jsonPath("$.lines[1].fulfillmentNode").value("STORE-114"))
                .andReturn().getResponse().getContentAsString();
        String orderNumber = JsonPath.read(body, "$.orderNumber");

        // Same key again (the browser retried): same order back, 200 not 201, nothing reserved twice.
        mvc.perform(post("/api/orders").header("Idempotency-Key", "chk-api-1")
                        .contentType(MediaType.APPLICATION_JSON).content(KETTLE_AND_SKILLET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));

        mvc.perform(post("/api/orders/" + orderNumber + "/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.lines[0].trackingNumber").value("TRK-STORE-114-" + orderNumber));
        assertThat(listeners.auditTrail()).contains("order.shipped | " + orderNumber + " [TRK-STORE-114-" + orderNumber + "]");

        // Rule: once the parcel has left, the order can no longer be cancelled.
        mvc.perform(post("/api/orders/" + orderNumber + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Illegal order state change"));
    }

    @Test
    void purchaseLimitIsEnforced() throws Exception {
        mvc.perform(post("/api/orders").header("Idempotency-Key", "chk-api-2")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"customerId":"CUST-4411","shipToRegion":"EAST","paymentToken":"tok_visa",
                                 "lines":[{"sku":"TOAST-2","quantity":3}]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("PURCHASE_LIMIT"))
                .andExpect(jsonPath("$.detail").value("At most 2 x TOAST-2 per order"));
    }

    @Test
    void outOfStockIsRejectedAndDeclinedPaymentHoldsNoStock() throws Exception {
        mvc.perform(post("/api/orders").header("Idempotency-Key", "chk-api-3")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"customerId":"CUST-4412","shipToRegion":"WEST","paymentToken":"tok_visa",
                                 "lines":[{"sku":"DOV-LTD","quantity":2}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("INSUFFICIENT_STOCK"));

        mvc.perform(post("/api/orders").header("Idempotency-Key", "chk-api-4")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"customerId":"CUST-4413","shipToRegion":"WEST","paymentToken":"tok_declined",
                                 "lines":[{"sku":"DOV-LTD","quantity":1}]}
                                """))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.rule").value("PAYMENT_DECLINED"));

        mvc.perform(get("/api/inventory/DOV-LTD"))
                .andExpect(jsonPath("$[0].nodeCode").value("STORE-207"))
                .andExpect(jsonPath("$[0].reserved").value(0));
    }
}
