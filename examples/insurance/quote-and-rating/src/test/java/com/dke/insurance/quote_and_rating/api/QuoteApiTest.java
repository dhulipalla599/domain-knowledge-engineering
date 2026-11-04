package com.dke.insurance.quote_and_rating.api;

import static org.hamcrest.Matchers.endsWith;
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
class QuoteApiTest {

    @Autowired
    MockMvc mvc;

    private static final String VALID = """
            {"driverName":"Priya Nair","driverAge":29,"state":"TX","vehicleType":"SEDAN","priorClaims":0}""";

    @Test
    void rateThenBindThenRebindIsRejected() throws Exception {
        String location = mvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RATED"))
                .andExpect(jsonPath("$.premium").isNumber())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(post(location + "/bind"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOUND"));

        mvc.perform(post(location + "/bind"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(endsWith("cannot move to BOUND")));
    }

    @Test
    void underageDriverIsDeclinedWith422() throws Exception {
        mvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("29", "17")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Driver must be at least 18 years old"));
    }

    @Test
    void malformedStateIs400() throws Exception {
        mvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("TX", "Texas")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void seededQuoteIsReadable() throws Exception {
        mvc.perform(get("/api/quotes/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverName").value("Maria Alvarez"));
    }
}
