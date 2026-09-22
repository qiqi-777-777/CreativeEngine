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

    /**
     * Answer a startup-related question inside a session, so that the
     * LangChain4j ChatMemory can keep multi-turn context on the server side.
     */
    String chatWithSession(String sessionId, String question, List<Map<String, Object>> history);
}
