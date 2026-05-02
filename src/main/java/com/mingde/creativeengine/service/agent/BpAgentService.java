package com.mingde.creativeengine.service.agent;

import java.util.Map;

/**
 * Lightweight agent for business plan writing.
 */
public interface BpAgentService {

    /**
     * Generate business plan content from project information.
     */
    String generateBp(Map<String, Object> projectInfo);
}
