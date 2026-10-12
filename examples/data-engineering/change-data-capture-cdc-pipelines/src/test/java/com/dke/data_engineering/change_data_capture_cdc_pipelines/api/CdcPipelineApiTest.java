package com.dke.data_engineering.change_data_capture_cdc_pipelines.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.PipelineRun;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.PipelineRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CdcPipelineApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PipelineRunRepository runs;

    private static final String BATCH = "{\"source\":\"customers-batch-1\"}";

    @Test
    void happyPathLoadsReplicaAndReplayIsIdempotent() throws Exception {
        mvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content(BATCH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.eventsRead").value(7))
                .andExpect(jsonPath("$.applied").value(3))
                .andExpect(jsonPath("$.skippedStale").value(2))
                .andExpect(jsonPath("$.rejected").value(2));

        mvc.perform(get("/api/replica/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].city").value("Porto"))
                .andExpect(jsonPath("$[1].city").value("Lagos"))
                .andExpect(jsonPath("$[2].deleted").value(true));

        mvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content(BATCH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applied").value(0))
                .andExpect(jsonPath("$.skippedStale").value(5));
    }

    @Test
    void rejectsUnsafeOrUnknownSourceNames() throws Exception {
        mvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"../application\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"no-such-log\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsSecondRunWhileOneIsRunning() throws Exception {
        PipelineRun running = PipelineRun.pending("customers-batch-1", "API");
        running.start();
        running = runs.save(running);
        try {
            mvc.perform(post("/api/pipeline/runs").contentType(MediaType.APPLICATION_JSON).content(BATCH))
                    .andExpect(status().isConflict());
        } finally {
            runs.delete(running);
        }
    }
}
