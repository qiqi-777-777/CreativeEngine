package com.mingde.creativeengine.service;

import com.mingde.creativeengine.entity.PolicyData;
import java.util.List;

/**
 * Policy data service.
 */
public interface PolicyDataService {

    List<PolicyData> getAllPolicies();

    PolicyData getPolicyById(Long id);

    PolicyData getPolicyByName(String policyName);

    List<PolicyData> searchPolicies(String keyword);
}
