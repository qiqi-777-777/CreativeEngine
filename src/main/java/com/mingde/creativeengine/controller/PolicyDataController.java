package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.aop.Idempotent;
import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.PolicyDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 政策数据控制器
 */
@Tag(name = "政策数据", description = "政策列表、详情与政策问答")
@RestController
@RequestMapping("/api/policy-data")
public class PolicyDataController {

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
    public Result<List<PolicyData>> getPolicyList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(policyDataService.searchPolicies(keyword));
    }

    /**
     * 政策列表（分页）
     *
     * <p>与 {@code /list} 并存：{@code /list} 是历史接口，小程序依赖它的 {@code data}
     * 是数组并且在客户端自己过滤关键词，直接改返回结构会打断它。
     * 新接入的调用方（以及数据量变大以后的小程序）用这个接口。</p>
     */
    @GetMapping("/page")
    public Result<PageResult<PolicyData>> getPolicyPage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(policyDataService.pagePolicies(keyword, Math.max(page, 1), clampSize(size)));
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    /**
     * 根据ID获取政策详情
     */
    @GetMapping("/detail/{id}")
    public Result<PolicyData> getPolicyDetail(@PathVariable Long id) {
        PolicyData policy = policyDataService.getPolicyById(id);
        if (policy == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "政策不存在");
        }
        return Result.success(policy);
    }

    /**
     * 政策问答
     */
    @Idempotent
    @PostMapping("/ask")
    public Result<String> askPolicy(@RequestBody Map<String, Object> params) {
        Long policyId = Long.valueOf(params.get("policyId").toString());
        String question = params.get("question").toString();

        // 获取政策内容
        PolicyData policy = policyDataService.getPolicyById(policyId);
        if (policy == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "政策不存在");
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
        return Result.success(deepSeekService.chat(prompt));
    }
}
