package com.dke.healthcare.electronic_health_records_ehr.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class EhrApiTest {

    @Autowired
    MockMvc mvc;

    private static final String BODY = "{\"authorId\":\"dr-lee\",\"text\":\"Chest pain resolved; ECG normal.\"}";

    private String createDraft(String patientId) throws Exception {
        String json = mvc.perform(post("/api/patients/" + patientId + "/notes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Object) com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    @Test
    void draftSignAmendAndReadChart() throws Exception {
        String id = createDraft("pat-jonas");

        mvc.perform(post("/api/notes/" + id + "/sign").contentType(MediaType.APPLICATION_JSON).content("{\"authorId\":\"dr-lee\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));

        mvc.perform(post("/api/notes/" + id + "/addenda").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":\"dr-lee\",\"text\":\"Troponin negative.\"}"))
                .andExpect(jsonPath("$.status").value("AMENDED"));

        mvc.perform(get("/api/patients/pat-jonas/chart").param("clinicianId", "dr-lee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].addenda[0]").value("Troponin negative."));
    }

    @Test
    void signedNoteRejectsEditsWith409() throws Exception {
        String id = createDraft("pat-amara");
        mvc.perform(post("/api/notes/" + id + "/sign").contentType(MediaType.APPLICATION_JSON).content("{\"authorId\":\"dr-lee\"}"));
        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void chartReadWithoutRelationshipIsForbiddenUnlessBreakGlass() throws Exception {
        mvc.perform(get("/api/patients/pat-priya/chart").param("clinicianId", "dr-lee"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/patients/pat-priya/chart").param("clinicianId", "dr-lee")
                        .param("breakGlassReason", "Unconscious in ED"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/patients/pat-priya/audit"))
                .andExpect(jsonPath("$[0].action").value("BREAK_GLASS_READ"));
    }
}
