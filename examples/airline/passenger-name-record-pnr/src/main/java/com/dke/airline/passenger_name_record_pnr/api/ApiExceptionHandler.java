package com.dke.airline.passenger_name_record_pnr.api;

import com.dke.airline.passenger_name_record_pnr.domain.RuleViolationException;
import com.dke.airline.passenger_name_record_pnr.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RuleViolationException.class)
    ProblemDetail onRule(RuleViolationException e) {
        HttpStatus status = e.kind() == RuleViolationException.Kind.CONFLICT
                ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
        return ProblemDetail.forStatusAndDetail(status, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail onNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).sorted().reduce((a, b) -> a + "; " + b).orElse("invalid request");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
