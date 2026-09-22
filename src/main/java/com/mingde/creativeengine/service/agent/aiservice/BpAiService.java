package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * BP 写作 Agent（LangChain4j AiService）。
 *
 * <p>项目字段较多且存在多种别名，因此在业务层统一整理成 projectBrief 数据块，
 * 由注解负责提示词的语气、结构与输出章节约束。</p>
 */
public interface BpAiService {

    @SystemMessage({
            "你是一名商业计划书写作助手，服务对象是大学生创业团队和早期项目团队。",
            "请根据用户提供的项目信息，生成结构清晰、表达专业、适合课程答辩或创业展示的 BP 内容。",
            "如果某些信息缺失，请基于已知信息谨慎补全，不要编造具体财务数据。"
    })
    @UserMessage({
            "项目信息：",
            "{{projectBrief}}",
            "",
            "补充要求：{{requirement}}",
            "",
            "请输出以下章节：",
            "1. 项目概述",
            "2. 市场痛点",
            "3. 产品方案",
            "4. 商业模式",
            "5. 竞争优势",
            "6. 团队优势",
            "7. 发展规划"
    })
    String generateBp(@V("projectBrief") String projectBrief,
                      @V("requirement") String requirement);

    /**
     * 未提供项目字段、仅有一句自然语言问题时使用。
     */
    @SystemMessage({
            "你是一名商业计划书写作顾问，服务对象是大学生创业团队和早期项目团队。",
            "请针对用户的问题给出可落地的写作建议，必要时说明还需要补充哪些项目信息。"
    })
    @UserMessage("用户问题：{{question}}")
    String answerQuestion(@V("question") String question);
}
