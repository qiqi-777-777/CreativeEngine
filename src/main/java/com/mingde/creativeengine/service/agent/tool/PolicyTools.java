package com.mingde.creativeengine.service.agent.tool;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.rag.PolicyRetriever;
import com.mingde.creativeengine.service.PolicyDataService;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 政策解读 Agent 可调用的工具集。
 *
 * <p>改造前是 Java 侧先把政策检索好、拼成数据块塞进提示词；现在把"查什么、看哪一条"
 * 交给模型自己决定：先 {@code searchPolicies} 定位，再 {@code getPolicyDetail} 取正文。</p>
 *
 * <p>两个设计约束：</p>
 * <ol>
 *   <li><b>工具内不再调用大模型</b>。工具只做数据查询，推理由外层 Agent 完成，
 *       否则一次问答会变成嵌套的多次模型调用，延迟和成本都不可控；</li>
 *   <li><b>工具不抛异常</b>。查询失败时把可读的失败原因作为工具结果回灌给模型，
 *       让模型改口说"暂时查不到"并提示以官方文件为准，而不是让整次问答直接失败。</li>
 * </ol>
 */
@Slf4j
@Component
public class PolicyTools {

    /** 单次检索返回的最大条数，避免把整个政策库灌进上下文。 */
    private static final int MAX_SEARCH_RESULT = 5;

    /** 单条政策正文的长度上限。 */
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final PolicyDataService policyDataService;

    /** 检索入口：默认走关键词，开启 RAG 后是「语义 + 关键词」混合召回，工具本身不感知差异。 */
    private final PolicyRetriever policyRetriever;

    public PolicyTools(PolicyDataService policyDataService, PolicyRetriever policyRetriever) {
        this.policyDataService = policyDataService;
        this.policyRetriever = policyRetriever;
    }

    @Tool("按问题检索创业政策库，返回最相关政策（语义 + 关键词混合召回）的 ID、标题与关键词。回答政策问题前应先调用本工具查证。")
    public String searchPolicies(@P("用户想了解的问题或关键词，例如：大学生创业补贴怎么申请、应届生能领哪些钱、税收减免") String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        try {
            List<PolicyData> policies = policyRetriever.retrieve(trimmed, MAX_SEARCH_RESULT);
            if (policies.isEmpty()) {
                return "政策库中没有检索到与「" + trimmed + "」相关的政策。";
            }

            StringBuilder result = new StringBuilder();
            result.append("按相关度检索到 ").append(policies.size()).append(" 条政策：\n");
            for (int i = 0; i < policies.size(); i++) {
                PolicyData policy = policies.get(i);
                result.append(i + 1)
                        .append(". [政策ID=").append(policy.getId()).append("] ")
                        .append(ToolSupport.defaultValue(policy.getPolicyName()))
                        .append("（关键词：").append(ToolSupport.defaultValue(policy.getKeywords())).append("）\n");
            }
            result.append("如需解读具体申报条件，请调用 getPolicyDetail 获取对应政策正文。");
            return result.toString();
        } catch (Exception e) {
            log.error("政策检索工具执行失败，keyword={}", keyword, e);
            return "政策库检索暂时不可用（" + e.getClass().getSimpleName() + "）。"
                    + "请明确告知用户当前无法查证政策细节，并建议以官方文件和主管部门通知为准，"
                    + "不要凭记忆给出具体条款或补贴金额。";
        }
    }

    /**
     * 参数用 String 接收而不是 Long：模型返回的 JSON 参数类型不稳定，
     * 字符串形式能兼容 12 与 "12" 两种写法，再由本方解析校验。
     */
    @Tool("根据政策 ID 获取政策正文，用于解读适用对象、申报条件与注意事项。")
    public String getPolicyDetail(@P("政策 ID，取值来自 searchPolicies 的返回结果") String policyId) {
        try {
            Long id = parseId(policyId);
            if (id == null) {
                return "政策 ID 无效（收到：" + policyId + "），请先调用 searchPolicies 获取有效的政策 ID。";
            }

            PolicyData policy = policyDataService.getPolicyById(id);
            if (policy == null) {
                return "没有找到 ID=" + id + " 的政策，请确认该 ID 是否来自 searchPolicies 的返回结果。";
            }

            return "标题：" + ToolSupport.defaultValue(policy.getPolicyName())
                    + "\n关键词：" + ToolSupport.defaultValue(policy.getKeywords())
                    + "\n正文：\n" + ToolSupport.truncate(policy.getContent(), MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.error("政策详情工具执行失败，policyId={}", policyId, e);
            return "政策详情查询暂时不可用（" + e.getClass().getSimpleName() + "）。"
                    + "请告知用户当前无法查证该政策的具体条款，并建议查阅官方文件。";
        }
    }

    private Long parseId(String policyId) {
        if (policyId == null || policyId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(policyId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
