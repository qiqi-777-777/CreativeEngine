package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.service.agent.CareerAgentService;
import com.mingde.creativeengine.service.agent.aiservice.CareerAiService;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 职业规划 Agent 实现。
 *
 * <p>职业库查询已下沉到 Agent 的工具（{@code OccupationTools.matchOccupations}），
 * 这里只负责把用户传入的标签规整后交给 AiService——用什么标签查、要不要先看可用标签，
 * 都由模型在工具调用里决定。</p>
 */
@Service
public class CareerAgentServiceImpl implements CareerAgentService {

    private final CareerAiService careerAiService;

    public CareerAgentServiceImpl(CareerAiService careerAiService) {
        this.careerAiService = careerAiService;
    }

    @Override
    public String suggestCareer(List<String> majors, List<String> skills, List<String> personalities) {
        List<String> safeMajors = safeList(majors);
        List<String> safeSkills = safeList(skills);
        List<String> safePersonalities = safeList(personalities);

        if (safeMajors.isEmpty() && safeSkills.isEmpty() && safePersonalities.isEmpty()) {
            return "请先选择专业、技能或性格标签，系统才能生成职业规划建议。";
        }

        return careerAiService.suggestCareer(
                formatList(safeMajors),
                formatList(safeSkills),
                formatList(safePersonalities)
        );
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private String formatList(List<String> values) {
        return values.isEmpty() ? "未提供" : String.join("、", values);
    }
}
