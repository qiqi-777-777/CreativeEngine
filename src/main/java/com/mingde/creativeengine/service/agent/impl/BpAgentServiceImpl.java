package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.agent.BpAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Business plan writing agent based on DeepSeek.
 */
@Service
public class BpAgentServiceImpl implements BpAgentService {

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public String generateBp(Map<String, Object> projectInfo) {
        if (projectInfo == null || projectInfo.isEmpty()) {
            return "请先填写项目名称、所属行业、目标用户、核心痛点和解决方案等信息。";
        }

        String prompt = buildPrompt(projectInfo);
        return deepSeekService.chat(prompt);
    }

    private String buildPrompt(Map<String, Object> projectInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名商业计划书写作助手，服务对象是大学生创业团队和早期项目团队。\n");
        prompt.append("请根据用户提供的项目信息，生成结构清晰、表达专业、适合课程答辩或创业展示的 BP 内容。\n");
        prompt.append("如果某些信息缺失，请基于已知信息谨慎补全，不要编造具体财务数据。\n\n");
        prompt.append("项目名称：").append(value(projectInfo, "projectName")).append("\n");
        prompt.append("一句话描述：").append(value(projectInfo, "oneLiner", "description")).append("\n");
        prompt.append("所属行业：").append(value(projectInfo, "industry")).append("\n");
        prompt.append("目标用户：").append(value(projectInfo, "targetUsers")).append("\n");
        prompt.append("核心痛点：").append(value(projectInfo, "painPoint", "painPoints")).append("\n");
        prompt.append("解决方案：").append(value(projectInfo, "solution", "oneLiner")).append("\n");
        prompt.append("核心竞争优势：").append(value(projectInfo, "competitiveEdge", "advantage")).append("\n");
        prompt.append("团队情况：").append(value(projectInfo, "team")).append("\n");
        prompt.append("商业模式：").append(value(projectInfo, "businessModel")).append("\n");
        prompt.append("融资需求：").append(value(projectInfo, "fundingNeeds")).append("\n\n");
        prompt.append("补充要求：").append(value(projectInfo, "question", "requirement")).append("\n\n");
        prompt.append("请输出以下章节：\n");
        prompt.append("1. 项目概述\n");
        prompt.append("2. 市场痛点\n");
        prompt.append("3. 产品方案\n");
        prompt.append("4. 商业模式\n");
        prompt.append("5. 竞争优势\n");
        prompt.append("6. 团队优势\n");
        prompt.append("7. 发展规划\n");
        return prompt.toString();
    }

    private String value(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "未提供";
    }
}
