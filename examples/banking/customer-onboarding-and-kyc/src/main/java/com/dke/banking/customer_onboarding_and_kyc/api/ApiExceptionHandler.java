package com.dke.banking.customer_onboarding_and_kyc.api;

import com.dke.banking.customer_onboarding_and_kyc.domain.BusinessRuleViolation;
import com.dke.banking.customer_onboarding_and_kyc.service.ApplicationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns domain errors into RFC 9457 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessRuleViolation.class)
    ProblemDetail businessRule(BusinessRuleViolation e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problem.setTitle("Business rule violated");
        problem.setProperty("code", e.getCode());
        return problem;
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    ProblemDetail notFound(ApplicationNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
