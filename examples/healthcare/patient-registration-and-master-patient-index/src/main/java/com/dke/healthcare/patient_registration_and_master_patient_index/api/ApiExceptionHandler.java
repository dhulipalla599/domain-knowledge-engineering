package com.dke.healthcare.patient_registration_and_master_patient_index.api;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.DuplicatePatientException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.IllegalStateTransitionException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.InvalidRegistrationException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DuplicatePatientException.class)
    ProblemDetail duplicate(DuplicatePatientException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Probable duplicate patient");
        p.setProperty("existingMrn", e.getExistingMrn());
        p.setProperty("score", e.getScore());
        return p;
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    ProblemDetail illegalTransition(IllegalStateTransitionException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Illegal record state");
        return p;
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    ProblemDetail invalid(InvalidRegistrationException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        p.setTitle("Invalid registration");
        return p;
    }

    @ExceptionHandler(PatientNotFoundException.class)
    ProblemDetail notFound(PatientNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
