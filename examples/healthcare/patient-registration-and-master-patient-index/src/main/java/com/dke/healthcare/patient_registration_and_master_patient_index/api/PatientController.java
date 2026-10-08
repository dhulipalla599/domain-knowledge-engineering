package com.dke.healthcare.patient_registration_and_master_patient_index.api;

import com.dke.healthcare.patient_registration_and_master_patient_index.service.RegistrationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PatientController {

    private final RegistrationService service;

    public PatientController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping("/patients")
    public ResponseEntity<PatientResponse> register(@Valid @RequestBody RegisterPatientRequest req) {
        var patient = service.register(req.firstName(), req.lastName(), req.dateOfBirth(), req.phone(), req.postcode());
        return ResponseEntity.created(URI.create("/api/patients/" + patient.getMrn()))
                .body(PatientResponse.from(patient));
    }

    @GetMapping("/patients/{mrn}")
    public PatientResponse get(@PathVariable String mrn) {
        return PatientResponse.from(service.find(mrn));
    }

    @GetMapping("/review-queue")
    public List<PatientResponse> reviewQueue() {
        return service.reviewQueue().stream().map(PatientResponse::from).toList();
    }

    @PostMapping("/patients/{mrn}/approve")
    public PatientResponse approve(@PathVariable String mrn) {
        return PatientResponse.from(service.approveAsDistinct(mrn));
    }

    @PostMapping("/patients/{mrn}/merge")
    public PatientResponse merge(@PathVariable String mrn, @Valid @RequestBody MergeRequest req) {
        return PatientResponse.from(service.merge(mrn, req.survivorMrn()));
    }
}
