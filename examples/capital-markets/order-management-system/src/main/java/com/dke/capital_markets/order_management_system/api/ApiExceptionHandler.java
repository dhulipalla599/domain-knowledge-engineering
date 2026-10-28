package com.dke.capital_markets.order_management_system.api;

import com.dke.capital_markets.order_management_system.service.NotFoundException;
import com.dke.capital_markets.order_management_system.service.RuleViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RuleViolationException.class)
    ProblemDetail ruleViolation(RuleViolationException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        p.setTitle("Order rejected by pre-trade check");
        p.setProperty("rule", e.getRule());
        return p;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Illegal lifecycle moves, e.g. cancelling a filled order or over-filling. */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    ProblemDetail conflict(RuntimeException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Illegal order state change");
        return p;
    }
}
