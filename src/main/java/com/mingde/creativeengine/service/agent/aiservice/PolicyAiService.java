package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 政策解读 Agent（LangChain4j AiService）。
 *
 * <p>系统提示与用户提示全部以注解声明，业务数据通过 {@code @V} 占位符注入，
 * 不再在 Java 代码里拼接提示词；模型调用、消息组装和返回值解析由
 * LangChain4j 的 {@code AiServices} 统一处理。</p>
 */
public interface PolicyAiService {

    /**
     * 基于单条政策的完整正文解读用户问题。
     */
    @SystemMessage({
            "你是一个面向大学生和青年创业者的政策解读助手。",
            "请严格基于给定的政策内容回答用户问题，不要编造政策中不存在的信息。",
            "如果政策内容无法支持结论，请明确说明需要以官方文件或主管部门解释为准。"
    })
    @UserMessage({
            "政策标题：{{policyName}}",
            "政策关键词：{{keywords}}",
            "政策内容：{{content}}",
            "",
            "用户问题：{{question}}",
            "",
            "请按照以下结构回答：",
            "1. 政策含义",
            "2. 适用对象",
            "3. 用户可能匹配的条件",
            "4. 申报建议",
            "5. 注意事项"
    })
    String askByPolicy(@V("policyName") String policyName,
                       @V("keywords") String keywords,
                       @V("content") String content,
                       @V("question") String question);

    /**
     * 基于政策库检索结果回答用户问题（未指定具体政策 ID 时使用）。
     */
    @SystemMessage({
            "你是一个面向大学生和青年创业者的政策解读 Agent。",
            "请基于给定的政策数据库检索结果回答用户问题，不要编造材料中不存在的政策细节。",
            "如果材料不足，请说明需要继续查看官方文件或主管部门通知。"
    })
    @UserMessage({
            "用户问题：{{question}}",
            "",
            "政策检索结果：",
            "{{policiesDigest}}",
            "",
            "请输出：",
            "1. 相关政策方向",
            "2. 用户可能关注的条件",
            "3. 申报或查询建议",
            "4. 风险提示"
    })
    String askBySearchResult(@V("question") String question,
                             @V("policiesDigest") String policiesDigest);

    /**
     * 政策库中没有检索到相关政策时的兜底回答。
     */
    @SystemMessage({
            "你是一个面向大学生和青年创业者的政策解读 Agent。",
            "当前政策库中没有检索到与该问题直接相关的政策。",
            "请注意提示用户以官方文件和主管部门通知为准，不要编造具体政策条款。"
    })
    @UserMessage("用户问题：{{question}}")
    String answerWithoutPolicy(@V("question") String question);
}
