package com.dke.logistics.shipment_booking.api;

import static org.hamcrest.Matchers.startsWith;
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
class BookingApiTest {

    @Autowired
    MockMvc mvc;

    private static String body(String quote, double kg) {
        return "{\"customerId\":\"C-77\",\"quoteId\":\"" + quote + "\",\"weightKg\":" + kg + "}";
    }

    @Test
    void happyPathConfirmsBookingAndIsIdempotent() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/bookings").header("Idempotency-Key", "api-1")
                            .contentType(MediaType.APPLICATION_JSON).content(body("Q-1001", 12000)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.carrierConfirmation", startsWith("NOR-")));
        }
    }

    @Test
    void expiredQuoteIsRejectedWith410() throws Exception {
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "api-2")
                        .contentType(MediaType.APPLICATION_JSON).content(body("Q-1003", 1000)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail", startsWith("Quote Q-1003 expired")));
    }

    @Test
    void overweightIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/bookings").header("Idempotency-Key", "api-3")
                        .contentType(MediaType.APPLICATION_JSON).content(body("Q-1001", 99999)))
                .andExpect(status().isUnprocessableEntity());
    }
}
