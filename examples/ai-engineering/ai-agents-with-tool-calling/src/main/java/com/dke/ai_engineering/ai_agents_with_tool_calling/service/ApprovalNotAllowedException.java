package com.dke.ai_engineering.ai_agents_with_tool_calling.service;

public class ApprovalNotAllowedException extends RuntimeException {
    public ApprovalNotAllowedException(String message) {
        super(message);
    }
}
