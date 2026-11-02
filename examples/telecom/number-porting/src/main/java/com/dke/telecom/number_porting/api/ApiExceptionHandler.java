package com.dke.telecom.number_porting.api;

import com.dke.telecom.number_porting.domain.IllegalPortStateException;
import com.dke.telecom.number_porting.service.PortRequestNotFound;
import com.dke.telecom.number_porting.service.PortRuleViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PortRuleViolation.class)
    ProblemDetail onRule(PortRuleViolation e) {
        HttpStatus status = "INVALID_PORT_DATE".equals(e.getCode()) ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.CONFLICT;
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        p.setTitle(e.getCode());
        return p;
    }

    @ExceptionHandler(IllegalPortStateException.class)
    ProblemDetail onState(IllegalPortStateException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("ILLEGAL_STATE_TRANSITION");
        return p;
    }

    @ExceptionHandler(PortRequestNotFound.class)
    ProblemDetail onNotFound(PortRequestNotFound e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
