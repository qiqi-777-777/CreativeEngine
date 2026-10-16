package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.aop.Idempotent;
import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.PolicyDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "政策数据", description = "政策列表、详情与政策问答")
@RestController
@RequestMapping("/api/policy-data")
public class PolicyDataController {

    private static final Logger log = LoggerFactory.getLogger(PolicyDataController.class);

    /** 单页最多返回条数，避免调用方传 size=100000 把这个接口当成全量导出用。 */
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private PolicyDataService policyDataService;

    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * 获取所有政策列表
     */
    @GetMapping("/list")
    public Map<String, Object> getPolicyList(@RequestParam(value = "keyword", required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<PolicyData> policies = policyDataService.searchPolicies(keyword);
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
     * 政策列表（分页）
     *
     * <p>与 {@code /list} 并存：{@code /list} 是历史接口，小程序依赖它的 {@code data}
     * 是数组并且在客户端自己过滤关键词，直接改返回结构会打断它。
     * 新接入的调用方（以及数据量变大以后的小程序）用这个接口。</p>
     */
    @GetMapping("/page")
    public Map<String, Object> getPolicyPage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            PageResult<PolicyData> policies =
                    policyDataService.pagePolicies(keyword, Math.max(page, 1), clampSize(size));
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", policies);
        } catch (Exception e) {
            log.error("分页获取政策列表失败", e);
            result.put("code", 500);
            result.put("message", "获取政策列表失败：" + e.getMessage());
        }
        return result;
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
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
    @Idempotent
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
