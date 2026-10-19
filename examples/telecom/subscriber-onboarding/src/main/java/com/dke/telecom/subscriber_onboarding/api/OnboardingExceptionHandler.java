package com.dke.telecom.subscriber_onboarding.api;

import com.dke.telecom.subscriber_onboarding.domain.IllegalTransitionException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OnboardingExceptionHandler {

    @ExceptionHandler(OnboardingException.class)
    ProblemDetail onRuleViolation(OnboardingException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getTitle());
        return problem;
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ProblemDetail onIllegalTransition(IllegalTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Illegal state transition");
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail onConcurrentUpdate(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The SIM or number was taken by another request; retry with a new choice");
        problem.setTitle("Concurrent update");
        return problem;
    }
}
