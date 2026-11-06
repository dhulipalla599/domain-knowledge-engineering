package com.dke.ai_engineering.ai_agents_with_tool_calling.service;

import java.util.Map;

/** A capability the agent may call. Implementations live in the integration package. */
public interface Tool {

    String name();

    String description();

    /** High-impact tools wait for a human decision before they run. */
    boolean requiresApproval();

    /** @throws IllegalArgumentException when the arguments are invalid; the message goes back to the model. */
    String execute(Map<String, String> arguments);
}
