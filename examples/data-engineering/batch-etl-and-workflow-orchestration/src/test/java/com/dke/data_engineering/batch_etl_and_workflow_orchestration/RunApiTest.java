package com.dke.data_engineering.batch_etl_and_workflow_orchestration;

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
class RunApiTest {

    @Autowired
    MockMvc mvc;

    private static String body(String date) {
        return "{\"businessDate\":\"" + date + "\"}";
    }

    @Test
    @Order(1)
    void happyPathLoadsValidRowsAndRejectsBadOnes() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-08")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.rowsExtracted").value(10))
                .andExpect(jsonPath("$.rowsLoaded").value(9))
                .andExpect(jsonPath("$.rowsRejected").value(1));
    }

    @Test
    @Order(2)
    void reloadingASucceededDateIsRejectedWith409() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-08")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Business date 2026-11-08 is already loaded"));
    }

    @Test
    @Order(3)
    void missingSourceFileIs404() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2020-01-01")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    void qualityGateFailsTheRunWithoutLoading() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content(body("2026-11-09")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.rowsLoaded").value(0));
    }

    @Test
    @Order(5)
    void missingDateIs400() throws Exception {
        mvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
