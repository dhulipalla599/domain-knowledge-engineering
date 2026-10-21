package com.dke.banking.core_banking_ledger_and_accounts.api;

import com.dke.banking.core_banking_ledger_and_accounts.domain.BusinessRuleViolation;
import com.dke.banking.core_banking_ledger_and_accounts.service.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
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

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Two concurrent requests with the same Idempotency-Key: the loser should simply retry. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicate(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Duplicate request in flight, retry shortly");
    }
}
