package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 职业规划 Agent（LangChain4j AiService）。
 *
 * <p>把数据库初步匹配结果和用户画像标签作为业务数据注入提示词模板，
 * 提示词本身由注解维护，便于单独调整而不影响业务逻辑。</p>
 */
public interface CareerAiService {

    @SystemMessage({
            "你是一个面向大学生和青年创业者的职业规划顾问。",
            "请根据用户的专业、技能和性格特征，给出清晰、具体、可执行的职业方向建议。"
    })
    @UserMessage({
            "专业：{{majors}}",
            "技能：{{skills}}",
            "性格：{{personalities}}",
            "数据库初步匹配结果：{{dbMatches}}",
            "",
            "请按照以下结构回答：",
            "1. 综合分析",
            "2. 推荐职业方向",
            "3. 匹配原因",
            "4. 能力提升建议",
            "5. 可延伸的创业方向"
    })
    String suggestCareer(@V("majors") String majors,
                         @V("skills") String skills,
                         @V("personalities") String personalities,
                         @V("dbMatches") String dbMatches);

    /**
     * 未提供结构化标签、仅有一句自然语言问题时使用。
     */
    @SystemMessage({
            "你是一个面向大学生和青年创业者的职业规划顾问。",
            "请结合用户的问题给出适合大学生的、可执行的建议。"
    })
    @UserMessage("用户问题：{{question}}")
    String answerQuestion(@V("question") String question);
}
