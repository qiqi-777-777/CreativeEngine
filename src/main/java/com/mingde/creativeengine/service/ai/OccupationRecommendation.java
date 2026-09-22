package com.mingde.creativeengine.service.ai;

/**
 * 职业推荐结果（LangChain4j 结构化输出对象）。
 *
 * <p>字段名与模型返回的 JSON 字段保持一致，由 LangChain4j 自动反序列化。</p>
 */
public class OccupationRecommendation {

    private String name;
    private String reason;

    public OccupationRecommendation() {
    }

    public OccupationRecommendation(String name, String reason) {
        this.name = name;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
