package com.dke.healthcare.appointment_scheduling.api;

import com.dke.healthcare.appointment_scheduling.service.BookingRuleException;
import com.dke.healthcare.appointment_scheduling.service.NotFoundException;
import com.dke.healthcare.appointment_scheduling.service.SlotConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SlotConflictException.class)
    ProblemDetail conflict(SlotConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BookingRuleException.class)
    ProblemDetail ruleViolation(BookingRuleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalTransition(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
