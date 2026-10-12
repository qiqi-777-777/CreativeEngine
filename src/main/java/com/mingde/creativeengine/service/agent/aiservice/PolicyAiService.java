package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 政策解读 Agent（LangChain4j AiService）。
 *
 * <p>两种入口对应两类上下文：</p>
 * <ul>
 *   <li>{@link #askByPolicy} —— 用户在界面上已经选定某条政策，上下文是确定的，
 *       直接注入正文即可，没必要让模型再去检索；</li>
 *   <li>{@link #askWithTools} —— 只有一句自然语言问题，查谁、看哪一条都需要判断，
 *       交给模型调用 {@code PolicyTools} 自己决定（先检索、再取正文）。</li>
 * </ul>
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
     * 只有自然语言问题时的政策咨询：由模型自行调用工具检索并取证。
     */
    @SystemMessage({
            "你是一个面向大学生和青年创业者的政策解读 Agent。",
            "你可以调用工具检索政策库、获取政策正文。回答政策问题前必须先调用工具查证，",
            "不要凭记忆编造政策条款、补贴金额、申报时间或主管部门名称。",
            "如果工具返回没有相关内容，请明确说明政策库中没有检索到，并建议用户以官方文件和主管部门通知为准。"
    })
    @UserMessage({
            "用户问题：{{question}}",
            "",
            "请先调用工具查证，再按以下结构回答：",
            "1. 相关政策方向",
            "2. 用户可能关注的条件",
            "3. 申报或查询建议",
            "4. 风险提示",
            "5. 你实际引用到的政策标题（必须来自工具返回结果，没有就说明未检索到）"
    })
    String askWithTools(@V("question") String question);
}
