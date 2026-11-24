package com.dke.data_engineering.data_quality_and_validation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ValidationApiTest {

    @Autowired
    MockMvc mvc;

    private static String body(String date) {
        return "{\"businessDate\":\"" + date + "\"}";
    }

    @Test
    @Order(1)
    void cleanEnoughFileIsLoadedWithBadRowQuarantined() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-23")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("LOADED"))
                .andExpect(jsonPath("$.rowsReceived").value(12))
                .andExpect(jsonPath("$.rowsLoaded").value(11))
                .andExpect(jsonPath("$.rowsRejected").value(1))
                .andExpect(jsonPath("$.rowsWarned").value(1));
        mvc.perform(get("/api/runs/2/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ruleCode").value("AMOUNT_POSITIVE"))
                .andExpect(jsonPath("$[1].ruleCode").value("HIGH_AMOUNT"));
    }

    @Test
    @Order(2)
    void sameFileTwiceIsRejectedWith409() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-23")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("File tpa-claims-2026-11-23.csv was already processed by run 2"));
    }

    @Test
    @Order(3)
    void fileOverTheGateIsQuarantinedAndNothingLoaded() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-24")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUARANTINED"))
                .andExpect(jsonPath("$.rowsRejected").value(4))
                .andExpect(jsonPath("$.rowsLoaded").value(0))
                .andExpect(jsonPath("$.errorRatio").value(0.4));
    }

    @Test
    @Order(4)
    void releaseNeedsAReasonThenLoadsValidRows() throws Exception {
        mvc.perform(post("/api/runs/3/release").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\":\"m.okafor\",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/runs/3/release").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\":\"m.okafor\",\"reason\":\"TPA confirmed the 4 bad rows will be resent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOADED"))
                .andExpect(jsonPath("$.rowsLoaded").value(6))
                .andExpect(jsonPath("$.releasedBy").value("m.okafor"));
    }

    @Test
    @Order(5)
    void releasingALoadedRunIs409() throws Exception {
        mvc.perform(post("/api/runs/2/release").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\":\"m.okafor\",\"reason\":\"Trying to release again\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(6)
    void schemaDriftFailsTheRun() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-25")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.rowsLoaded").value(0));
    }

    @Test
    @Order(7)
    void missingFileIs404AndMissingDateIs400() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2020-01-01")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
