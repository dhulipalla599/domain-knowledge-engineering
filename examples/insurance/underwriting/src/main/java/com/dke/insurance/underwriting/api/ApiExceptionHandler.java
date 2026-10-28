package com.dke.insurance.underwriting.api;

import com.dke.insurance.underwriting.domain.IllegalTransitionException;
import com.dke.insurance.underwriting.service.RuleViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RuleViolationException.class)
    ProblemDetail onRuleViolation(RuleViolationException ex) {
        return ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ProblemDetail onIllegalTransition(IllegalTransitionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalid(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldError();
        String detail = field == null ? "Invalid request" : field.getField() + " " + field.getDefaultMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
