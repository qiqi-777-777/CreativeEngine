package com.mingde.creativeengine.service.agent;

import java.util.Map;

/**
 * Lightweight router agent for dispatching user questions to scene agents.
 */
public interface RouterAgentService {

    /**
     * Route a user question to the most suitable scene agent.
     */
    Map<String, Object> route(String question, Map<String, Object> context);
}
