package com.dke.banking.payments_processing_ach_wire_rtp.api;

import com.dke.banking.payments_processing_ach_wire_rtp.service.PaymentRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns rule violations into RFC 9457 problem responses; validation errors come from the parent class. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PaymentRuleException.class)
    ProblemDetail onRule(PaymentRuleException e) {
        return ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail onIllegalState(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
