package com.mingde.creativeengine.service.agent;

/**
 * Lightweight agent for policy explanation.
 */
public interface PolicyAgentService {

    /**
     * Explain a policy and answer a question based on stored policy data.
     */
    String askPolicy(Long policyId, String question);
}
