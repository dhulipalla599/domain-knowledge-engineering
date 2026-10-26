package com.dke.data_engineering.real_time_streaming_etl_with_kafka.api;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.service.RunAlreadyInProgressException;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.service.UnknownSourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownSourceException.class)
    ProblemDetail notFound(UnknownSourceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RunAlreadyInProgressException.class)
    ProblemDetail conflict(RunAlreadyInProgressException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "source must not be blank");
    }
}
