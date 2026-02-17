package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.PolicyDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 政策数据控制器
 */
@RestController
@RequestMapping("/api/policy-data")
public class PolicyDataController {
    
    private static final Logger log = LoggerFactory.getLogger(PolicyDataController.class);
    
    @Autowired
    private PolicyDataService policyDataService;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    /**
     * 获取所有政策列表
     */
    @GetMapping("/list")
    public Map<String, Object> getPolicyList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<PolicyData> policies = policyDataService.getAllPolicies();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", policies);
        } catch (Exception e) {
            log.error("获取政策列表失败", e);
            result.put("code", 500);
            result.put("message", "获取政策列表失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 根据ID获取政策详情
     */
    @GetMapping("/detail/{id}")
    public Map<String, Object> getPolicyDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            PolicyData policy = policyDataService.getPolicyById(id);
            if (policy != null) {
                result.put("code", 200);
                result.put("message", "success");
                result.put("data", policy);
            } else {
                result.put("code", 404);
                result.put("message", "政策不存在");
            }
        } catch (Exception e) {
            log.error("获取政策详情失败", e);
            result.put("code", 500);
            result.put("message", "获取政策详情失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 政策问答
     */
    @PostMapping("/ask")
    public Map<String, Object> askPolicy(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long policyId = Long.valueOf(params.get("policyId").toString());
            String question = params.get("question").toString();
            
            // 获取政策内容
            PolicyData policy = policyDataService.getPolicyById(policyId);
            if (policy == null) {
                result.put("code", 404);
                result.put("message", "政策不存在");
                return result;
            }
            
            // 构建提示词，将政策内容作为上下文
            String prompt = String.format(
                "你是一个政策解读助手。以下是政策信息：\n\n" +
                "政策名称：%s\n" +
                "关键词：%s\n" +
                "政策内容：%s\n\n" +
                "用户问题：%s\n\n" +
                "请基于上述政策信息，简洁准确地回答用户的问题。如果问题与该政策无关，请礼貌地说明。",
                policy.getPolicyName(),
                policy.getKeywords(),
                policy.getContent(),
                question
            );
            
            // 调用DeepSeek API
            String answer = deepSeekService.chat(prompt);
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", answer);
            
        } catch (Exception e) {
            log.error("政策问答失败", e);
            result.put("code", 500);
            result.put("message", "问答失败：" + e.getMessage());
        }
        return result;
    }
}
