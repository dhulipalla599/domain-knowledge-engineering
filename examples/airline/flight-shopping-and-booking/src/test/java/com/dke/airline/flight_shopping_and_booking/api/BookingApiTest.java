package com.dke.airline.flight_shopping_and_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.airline.flight_shopping_and_booking.integration.BookingAuditListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    private static final String FLEX = "aaaaaaaa-0000-0000-0000-000000000002";
    private static final String SAVER_LAST_SEAT = "aaaaaaaa-0000-0000-0000-000000000003";
    private static final String SOLD_OUT = "aaaaaaaa-0000-0000-0000-000000000004";

    @Autowired
    MockMvc mvc;

    @Autowired
    BookingAuditListener audit;

    private static String hold(String offerId, String price) {
        return """
                {"offerId":"%s","passengerName":"Amara Osei","passengerEmail":"amara@example.com","expectedPrice":%s}
                """.formatted(offerId, price);
    }

    private String idOf(String json) {
        return json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void searchReturnsOnlySellableOffersCheapestFirst() throws Exception {
        mvc.perform(get("/api/v1/offers").param("origin", "sfo").param("destination", "JFK").param("date", "2027-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fareClass").value("ECONOMY_SAVER"));
    }

    @Test
    void happyPathHoldsPaysAndTickets() throws Exception {
        var created = mvc.perform(post("/api/v1/bookings").header("Idempotency-Key", "t-happy")
                        .contentType(MediaType.APPLICATION_JSON).content(hold(FLEX, "429.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andReturn().getResponse().getContentAsString();
        String id = idOf(created);

        mvc.perform(post("/api/v1/bookings/" + id + "/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentToken\":\"tok_ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TICKETED"))
                .andExpect(jsonPath("$.pnr").exists());

        assertThat(audit.entries()).anyMatch(e -> e.startsWith("flight.booking.held") && e.contains(id));
        assertThat(audit.entries()).anyMatch(e -> e.startsWith("flight.booking.ticketed") && e.contains(id));
    }

    @Test
    void replayedIdempotencyKeyReturnsSameBookingWithoutTakingAnotherSeat() throws Exception {
        var first = mvc.perform(post("/api/v1/bookings").header("Idempotency-Key", "t-replay")
                        .contentType(MediaType.APPLICATION_JSON).content(hold(FLEX, "429.00")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var second = mvc.perform(post("/api/v1/bookings").header("Idempotency-Key", "t-replay")
                        .contentType(MediaType.APPLICATION_JSON).content(hold(FLEX, "429.00")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(idOf(second)).isEqualTo(idOf(first));
    }

    @Test
    void staleFareIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/v1/bookings").header("Idempotency-Key", "t-fare")
                        .contentType(MediaType.APPLICATION_JSON).content(hold(FLEX, "399.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Fare changed")));
    }

    @Test
    void soldOutFareIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/v1/bookings").header("Idempotency-Key", "t-soldout")
                        .contentType(MediaType.APPLICATION_JSON).content(hold(SOLD_OUT, "1180.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("No seats left")));
    }

    @Test
    void declinedPaymentKeepsHoldAndCancelFreesTheLastSeat() throws Exception {
        var created = mvc.perform(post("/api/v1/bookings").header("Idempotency-Key", "t-decline")
                        .contentType(MediaType.APPLICATION_JSON).content(hold(SAVER_LAST_SEAT, "312.50")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = idOf(created);

        mvc.perform(post("/api/v1/bookings/" + id + "/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentToken\":\"tok_declined\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/bookings/" + id)).andExpect(jsonPath("$.status").value("HELD"));

        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"customer changed plans\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        // A second cancel is an illegal transition.
        mvc.perform(post("/api/v1/bookings/" + id + "/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"again\"}"))
                .andExpect(status().isConflict());
    }
}
