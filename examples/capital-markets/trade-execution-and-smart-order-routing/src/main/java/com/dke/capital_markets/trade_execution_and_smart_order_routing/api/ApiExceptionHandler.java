package com.dke.capital_markets.trade_execution_and_smart_order_routing.api;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.service.BusinessRuleException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException e) {
        HttpStatus status = e.isConflict() ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        p.setTitle("Order rejected by pre-trade check");
        p.setProperty("rule", e.getRule());
        return p;
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Illegal lifecycle moves, e.g. cancelling a FILLED order. */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalState(IllegalStateException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Illegal order state change");
        return p;
    }
}
