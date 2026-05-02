package com.mingde.creativeengine.service.agent;

import java.util.List;
import java.util.Map;

/**
 * Lightweight agent for startup consulting.
 */
public interface StartupAgentService {

    /**
     * Answer a startup-related question.
     */
    String chat(String question);

    /**
     * Answer a startup-related question with recent conversation history.
     */
    String chatWithHistory(String question, List<Map<String, Object>> history);
}
