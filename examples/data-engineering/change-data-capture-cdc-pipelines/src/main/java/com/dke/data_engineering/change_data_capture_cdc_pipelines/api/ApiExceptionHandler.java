package com.dke.data_engineering.change_data_capture_cdc_pipelines.api;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.RunAlreadyInProgressException;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.UnknownSourceException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RunAlreadyInProgressException.class)
    ProblemDetail inProgress(RunAlreadyInProgressException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({UnknownSourceException.class, NoSuchElementException.class})
    ProblemDetail notFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("invalid request");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
