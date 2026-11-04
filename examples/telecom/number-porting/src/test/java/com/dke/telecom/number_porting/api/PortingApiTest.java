package com.dke.telecom.number_porting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dke.telecom.number_porting.integration.InMemoryRoutingGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PortingApiTest.FixedClock.class)
class PortingApiTest {

    /** "Today" is Monday 2026-11-02. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired InMemoryRoutingGateway routing;

    private String body(String msisdn, String name, String date) {
        return """
            {"msisdn":"%s","subscriberName":"%s","donorOperator":"Contoso Telecom","requestedPortDate":"%s"}
            """.formatted(msisdn, name, date);
    }

    @Test
    void happyPathSubmitIsIdempotent() throws Exception {
        mvc.perform(post("/api/port-requests").header("Idempotency-Key", "t-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("+15550101001", "Ada Lovelace", "2026-11-03")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mvc.perform(post("/api/port-requests").header("Idempotency-Key", "t-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("+15550101001", "Ada Lovelace", "2026-11-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void weekendPortDateIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/port-requests").header("Idempotency-Key", "t-2")
                        .contentType(MediaType.APPLICATION_JSON).content(body("+15550101002", "Grace Hopper", "2026-11-07")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("INVALID_PORT_DATE"));
    }

    @Test
    void donorRejectionIsRecordedAndSecondInFlightPortIsConflict() throws Exception {
        mvc.perform(post("/api/port-requests").header("Idempotency-Key", "t-3")
                        .contentType(MediaType.APPLICATION_JSON).content(body("+15550101003", "Alan Turing", "2026-11-03")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("CONTRACT_LOCK_ACTIVE"));

        mvc.perform(post("/api/port-requests").header("Idempotency-Key", "t-4")
                        .contentType(MediaType.APPLICATION_JSON).content(body("+15550109001", "Maria Santos", "2026-11-03")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("DUPLICATE_PORT"));
    }

    @Test
    void completeBeforePortDateIsConflictAndSeededCompletedCannotBeCancelled() throws Exception {
        mvc.perform(post("/api/port-requests/1/complete")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("TOO_EARLY"));
        mvc.perform(post("/api/port-requests/2/cancel")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("ILLEGAL_STATE_TRANSITION"));
        assertEquals(java.util.Optional.empty(), routing.routeOf("+15550109001"));
    }
}
