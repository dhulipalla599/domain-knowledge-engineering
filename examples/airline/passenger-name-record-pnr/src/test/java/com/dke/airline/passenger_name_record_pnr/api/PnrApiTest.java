package com.dke.airline.passenger_name_record_pnr.api;

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
class PnrApiTest {

    @Autowired
    MockMvc mvc;

    private static String body(String first, String last, String flight) {
        return """
                {"contactEmail":"t@example.com",
                 "passengers":[{"firstName":"%s","lastName":"%s"}],
                 "segments":[{"flightNumber":"%s","departureDate":"2027-03-01","origin":"LHR","destination":"CDG"}]}
                """.formatted(first, last, flight);
    }

    @Test
    void createTicketAndRetrieve() throws Exception {
        String json = mvc.perform(post("/api/pnrs").contentType(MediaType.APPLICATION_JSON).content(body("mia", "chen", "SK300")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ON_HOLD"))
                .andExpect(jsonPath("$.passengers[0].lastName").value("CHEN"))
                .andReturn().getResponse().getContentAsString();
        String locator = com.jayway.jsonpath.JsonPath.read(json, "$.recordLocator");

        mvc.perform(post("/api/pnrs/" + locator + "/ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TICKETED"))
                .andExpect(jsonPath("$.ticketNumbers[0]").value("176-" + locator + "-1"));
        mvc.perform(get("/api/pnrs/" + locator)).andExpect(status().isOk());
    }

    @Test
    void duplicateBookingOnSameFlightIsRejected() throws Exception {
        mvc.perform(post("/api/pnrs").contentType(MediaType.APPLICATION_JSON).content(body("Noor", "Rahman", "SK400")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/pnrs").contentType(MediaType.APPLICATION_JSON).content(body("NOOR", "RAHMAN", "SK400")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Duplicate booking")));
    }

    @Test
    void ticketingAnAlreadyTicketedPnrIsConflict() throws Exception {
        mvc.perform(post("/api/pnrs/R4T9WZ/ticket")).andExpect(status().isConflict());
    }
}
