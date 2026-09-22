package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.service.agent.BpAgentService;
import com.mingde.creativeengine.service.agent.aiservice.BpAiService;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * BP 写作 Agent 实现：先把项目字段整理成数据块，再交给
 * {@link BpAiService} 按声明好的章节结构生成内容。
 */
@Service
public class BpAgentServiceImpl implements BpAgentService {

    private final BpAiService bpAiService;

    public BpAgentServiceImpl(BpAiService bpAiService) {
        this.bpAiService = bpAiService;
    }

    @Override
    public String generateBp(Map<String, Object> projectInfo) {
        if (projectInfo == null || projectInfo.isEmpty()) {
            return "请先填写项目名称、所属行业、目标用户、核心痛点和解决方案等信息。";
        }

        return bpAiService.generateBp(buildProjectBrief(projectInfo),
                value(projectInfo, "question", "requirement"));
    }

    /**
     * 项目字段本身存在多种别名，统一在这里整理为「字段：取值」的数据块，
     * 提示词的语气与输出结构由 @SystemMessage / @UserMessage 维护。
     */
    private String buildProjectBrief(Map<String, Object> projectInfo) {
        StringBuilder brief = new StringBuilder();
        brief.append("项目名称：").append(value(projectInfo, "projectName")).append("\n");
        brief.append("一句话描述：").append(value(projectInfo, "oneLiner", "description")).append("\n");
        brief.append("所属行业：").append(value(projectInfo, "industry")).append("\n");
        brief.append("目标用户：").append(value(projectInfo, "targetUsers")).append("\n");
        brief.append("核心痛点：").append(value(projectInfo, "painPoint", "painPoints")).append("\n");
        brief.append("解决方案：").append(value(projectInfo, "solution", "oneLiner")).append("\n");
        brief.append("核心竞争优势：").append(value(projectInfo, "competitiveEdge", "advantage")).append("\n");
        brief.append("团队情况：").append(value(projectInfo, "team")).append("\n");
        brief.append("商业模式：").append(value(projectInfo, "businessModel")).append("\n");
        brief.append("融资需求：").append(value(projectInfo, "fundingNeeds"));
        return brief.toString();
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
