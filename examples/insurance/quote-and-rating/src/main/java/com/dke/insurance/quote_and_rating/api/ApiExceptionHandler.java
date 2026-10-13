package com.dke.insurance.quote_and_rating.api;

import com.dke.insurance.quote_and_rating.domain.QuoteRuleException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns rule violations into RFC 9457 problem responses; bean-validation errors (400) come from the base class. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(QuoteRuleException.class)
    ProblemDetail onRuleViolation(QuoteRuleException ex) {
        return ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
    }
}
