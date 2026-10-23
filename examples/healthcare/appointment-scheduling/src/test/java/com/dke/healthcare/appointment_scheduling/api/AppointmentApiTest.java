package com.dke.healthcare.appointment_scheduling.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentApiTest {

    @Autowired
    MockMvc mvc;

    private String body(String patient, String provider, Instant start) {
        return "{\"patientId\":\"%s\",\"providerId\":\"%s\",\"startTime\":\"%s\",\"reason\":\"checkup\"}"
                .formatted(patient, provider, start);
    }

    private Instant slot(int days) {
        return Instant.now().plus(Duration.ofDays(days)).truncatedTo(ChronoUnit.DAYS).plus(Duration.ofHours(9));
    }

    @Test
    void booksThenRejectsDoubleBookingAndIsIdempotent() throws Exception {
        Instant start = slot(5);
        mvc.perform(post("/api/appointments").header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("pat-amara", "prov-chen", start)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        mvc.perform(post("/api/appointments").header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("pat-amara", "prov-chen", start)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(body("pat-jonas", "prov-chen", start)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());

        mvc.perform(get("/api/appointments").param("providerId", "prov-chen"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsLapsedInsuranceAndIllegalTransition() throws Exception {
        mvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(body("pat-lapsed", "prov-ortiz", slot(6))))
                .andExpect(status().isUnprocessableEntity());

        String json = mvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(body("pat-amara", "prov-ortiz", slot(7))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/appointments/" + id + "/cancel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/appointments/" + id + "/check-in")).andExpect(status().isConflict());
    }
}
