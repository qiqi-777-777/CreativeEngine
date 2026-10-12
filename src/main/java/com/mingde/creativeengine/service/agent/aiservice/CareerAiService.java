package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 职业规划 Agent（LangChain4j AiService）。
 *
 * <p>职业库匹配不再由 Java 侧预先跑好塞进提示词，而是通过 {@code OccupationTools}
 * 交给模型自己查：用户口语化描述时，模型可以先去工具里看有哪些标准标签，
 * 再决定用什么标签匹配。</p>
 */
public interface CareerAiService {

    @SystemMessage({
            "你是一个面向大学生和青年创业者的职业规划顾问。",
            "你可以调用工具查询职业库。回答前应先调用工具确认匹配的职业方向，不要编造职业库中不存在的岗位。",
            "如果用户的描述比较口语化，可以先调用工具查看可用标签，把描述映射为标准标签后再匹配。"
    })
    @UserMessage({
            "专业：{{majors}}",
            "技能：{{skills}}",
            "性格：{{personalities}}",
            "",
            "请先调用工具查询匹配职业，再按以下结构回答：",
            "1. 综合分析",
            "2. 推荐职业方向（优先采用职业库中的匹配结果，可补充说明其他可能方向）",
            "3. 匹配原因",
            "4. 能力提升建议",
            "5. 可延伸的创业方向"
    })
    String suggestCareer(@V("majors") String majors,
                         @V("skills") String skills,
                         @V("personalities") String personalities);

    /**
     * 未提供结构化标签、仅有一句自然语言问题时使用。
     */
    @SystemMessage({
            "你是一个面向大学生和青年创业者的职业规划顾问。",
            "请结合用户的问题给出适合大学生的、可执行的建议。",
            "如果问题涉及具体岗位、行业或职业匹配，请先调用工具查询职业库再回答，不要编造岗位信息。"
    })
    @UserMessage({
            "用户问题：{{question}}",
            "",
            "如果涉及具体岗位或行业，请先调用工具查询职业库再回答。"
    })
    String answerQuestion(@V("question") String question);
}
