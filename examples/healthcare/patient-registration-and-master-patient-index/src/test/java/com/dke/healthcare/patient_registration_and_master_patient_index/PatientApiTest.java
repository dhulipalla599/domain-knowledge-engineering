package com.dke.healthcare.patient_registration_and_master_patient_index;

import static org.hamcrest.Matchers.startsWith;
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
class PatientApiTest {

    @Autowired
    MockMvc mvc;

    private static String body(String first, String last, String dob, String phone) {
        return """
                {"firstName":"%s","lastName":"%s","dateOfBirth":"%s","phone":"%s","postcode":"CF101AA"}"""
                .formatted(first, last, dob, phone);
    }

    @Test
    void newPatientIsRegisteredAsActive() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Noor", "Haddad", "1990-01-05", "5550199000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.mrn", startsWith("MRN-")));
    }

    @Test
    void probableDuplicateIsRejectedWithConflict() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Liam", "Hartley", "1975-11-02", "555-010-7788")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingMrn").value("MRN-00000002"));
    }

    @Test
    void possibleMatchGoesToReviewQueue() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Priya", "Ramen", "1992-07-30", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(get("/api/review-queue")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.lastName=='Ramen')]").exists());
    }

    @Test
    void futureBirthDateIsUnprocessable() throws Exception {
        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Zed", "Future", "2999-01-01", "")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void mergingARecordIntoItselfIsRejected() throws Exception {
        mvc.perform(post("/api/patients/MRN-00000003/merge").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"survivorMrn\":\"MRN-00000003\"}"))
                .andExpect(status().isConflict());
    }
}
