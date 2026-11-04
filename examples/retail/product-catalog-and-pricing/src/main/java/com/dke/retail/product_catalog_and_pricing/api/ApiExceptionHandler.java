package com.dke.retail.product_catalog_and_pricing.api;

import com.dke.retail.product_catalog_and_pricing.service.PricingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PricingException.NotFound.class)
    ProblemDetail notFound(PricingException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({PricingException.BelowMarginFloor.class, PricingException.InvalidPrice.class})
    ProblemDetail unprocessable(PricingException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler({PricingException.Conflict.class, IllegalStateException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
