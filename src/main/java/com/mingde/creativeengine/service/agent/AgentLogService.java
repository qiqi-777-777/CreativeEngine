package com.mingde.creativeengine.service.agent;

import java.util.List;
import java.util.Map;

/**
 * Persistence service for agent call history.
 */
public interface AgentLogService {

    void saveLog(Long userId, String agentType, String inputText, String outputText, String relatedId);

    List<Map<String, Object>> listRecentLogs(Long userId, int limit);
}
