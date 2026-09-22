package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 职业推荐 Agent（LangChain4j AiService）。
 *
 * <p>DeepSeek 只支持 {@code response_format: json_object}，不支持严格的 JSON-Schema，
 * 因此这里返回原始 JSON 文本，由调用方解析；模型侧通过独立的 JSON 模式 ChatModel
 * 保证输出是合法 JSON。</p>
 */
public interface OccupationAiService {

    @SystemMessage({
            "你是一个职业规划专家，需要根据用户画像给出匹配度高的具体职业。",
            "你只能输出 json，不要输出任何解释性文字。"
    })
    @UserMessage({
            "专业：{{majors}}",
            "技能：{{skills}}",
            "性格：{{personalities}}",
            "",
            "请返回 {{count}} 个职业推荐，格式为：",
            "{\"recommendations\":[{\"name\":\"职业名称\",\"reason\":\"一句话推荐理由，30字以内\"}]}",
            "",
            "要求：职业要具体，不要过于宽泛；推荐理由要突出匹配点。"
    })
    String recommendJson(@V("majors") String majors,
                         @V("skills") String skills,
                         @V("personalities") String personalities,
                         @V("count") int count);
}
