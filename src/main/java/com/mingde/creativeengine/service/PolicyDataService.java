package com.mingde.creativeengine.service;

import com.mingde.creativeengine.entity.PolicyData;
import java.util.List;

/**
 * 政策数据服务接口
 */
public interface PolicyDataService {
    
    /**
     * 获取所有政策列表
     */
    List<PolicyData> getAllPolicies();
    
    /**
     * 根据ID获取政策详情
     */
    PolicyData getPolicyById(Long id);
    
    /**
     * 根据政策名称获取政策
     */
    PolicyData getPolicyByName(String policyName);
}
