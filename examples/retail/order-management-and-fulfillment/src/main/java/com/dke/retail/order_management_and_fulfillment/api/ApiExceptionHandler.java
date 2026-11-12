package com.dke.retail.order_management_and_fulfillment.api;

import com.dke.retail.order_management_and_fulfillment.service.BusinessRuleException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        p.setTitle("Order rejected");
        p.setProperty("rule", e.getRule());
        return p;
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Illegal lifecycle moves, e.g. cancelling an order that has already SHIPPED. */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalState(IllegalStateException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Illegal order state change");
        return p;
    }
}
