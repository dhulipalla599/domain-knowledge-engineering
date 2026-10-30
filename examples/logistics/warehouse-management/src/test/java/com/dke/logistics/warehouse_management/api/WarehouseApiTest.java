package com.dke.logistics.warehouse_management.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WarehouseApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void receiveThenPutAwayHappyPath() throws Exception {
        mvc.perform(post("/api/receipts/1/receive").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"countedQty\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mvc.perform(post("/api/receipts/1/putaway").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"binCode\":\"A-01-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUT_AWAY"))
                .andExpect(jsonPath("$.binCode").value("A-01-01"));

        mvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(200));
    }

    @Test
    void overReceiptIsRejectedWith422() throws Exception {
        mvc.perform(post("/api/receipts/3/receive").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"countedQty\":80}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Over-receipt")));
    }

    @Test
    void putAwayIntoFullBinIsRejectedWith409() throws Exception {
        mvc.perform(post("/api/receipts/1/receive").contentType(MediaType.APPLICATION_JSON)
                .content("{\"countedQty\":200}")).andExpect(status().isOk());

        mvc.perform(post("/api/receipts/1/putaway").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"binCode\":\"A-01-02\"}"))
                .andExpect(status().isConflict());
    }
}
