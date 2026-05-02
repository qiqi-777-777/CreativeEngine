package com.mingde.creativeengine.service.agent;

import java.util.List;

/**
 * Lightweight agent for career planning.
 */
public interface CareerAgentService {

    /**
     * Generate career planning advice from selected profile labels.
     */
    String suggestCareer(List<String> majors, List<String> skills, List<String> personalities);
}
