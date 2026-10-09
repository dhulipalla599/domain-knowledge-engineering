package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.api;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.RagService.IdempotencyConflictException;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.RagService.PromptInjectionException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps business-rule violations to RFC 7807 problem responses. Bean-validation errors (400) come from the base class. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PromptInjectionException.class)
    ProblemDetail promptInjection(PromptInjectionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler({IdempotencyConflictException.class, IllegalStateException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
