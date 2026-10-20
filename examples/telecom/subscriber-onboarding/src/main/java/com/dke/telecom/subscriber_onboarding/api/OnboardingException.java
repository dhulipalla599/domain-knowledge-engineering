package com.dke.telecom.subscriber_onboarding.api;

import org.springframework.http.HttpStatus;

public class OnboardingException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    public OnboardingException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() { return status; }
    public String getTitle() { return title; }
}
