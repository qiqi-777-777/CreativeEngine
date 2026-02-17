package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;
import com.mingde.creativeengine.service.PolicyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 政策数据服务实现
 */
@Service
public class PolicyDataServiceImpl implements PolicyDataService {
    
    @Autowired
    private PolicyDataMapper policyDataMapper;
    
    @Override
    public List<PolicyData> getAllPolicies() {
        return policyDataMapper.selectList(null);
    }
    
    @Override
    public PolicyData getPolicyById(Long id) {
        return policyDataMapper.selectById(id);
    }
    
    @Override
    public PolicyData getPolicyByName(String policyName) {
        LambdaQueryWrapper<PolicyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyData::getPolicyName, policyName);
        return policyDataMapper.selectOne(wrapper);
    }
}
