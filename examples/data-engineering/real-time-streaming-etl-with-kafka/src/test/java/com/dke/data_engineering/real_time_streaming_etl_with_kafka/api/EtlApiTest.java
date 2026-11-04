package com.dke.data_engineering.real_time_streaming_etl_with_kafka.api;

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
class EtlApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void batchOneLoadsValidRowsSkipsDuplicatesAndDeadLettersBadRows() throws Exception {
        mvc.perform(post("/api/pipeline-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"orders-batch-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_REJECTS"))
                .andExpect(jsonPath("$.recordsRead").value(10))
                .andExpect(jsonPath("$.recordsLoaded").value(4))
                .andExpect(jsonPath("$.duplicatesSkipped").value(2))
                .andExpect(jsonPath("$.recordsRejected").value(4));

        mvc.perform(get("/api/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GB").value(12000));
    }

    @Test
    void unknownSourceIsRejectedWithNotFoundProblem() throws Exception {
        mvc.perform(post("/api/pipeline-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Unknown source: nope"));
    }

    @Test
    void blankSourceIsABadRequest() throws Exception {
        mvc.perform(post("/api/pipeline-runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
