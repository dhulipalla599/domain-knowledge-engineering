package com.dke.ai_engineering.ai_agents_with_tool_calling.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
