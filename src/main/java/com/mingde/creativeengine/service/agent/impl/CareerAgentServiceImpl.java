package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.service.OccupationService;
import com.mingde.creativeengine.service.agent.CareerAgentService;
import com.mingde.creativeengine.service.agent.aiservice.CareerAiService;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 职业规划 Agent 实现：先用数据库匹配得到初步结果，再作为业务数据
 * 注入 {@link CareerAiService} 的提示词模板，由 LangChain4j 完成调用。
 */
@Service
public class CareerAgentServiceImpl implements CareerAgentService {

    private final OccupationService occupationService;
    private final CareerAiService careerAiService;

    public CareerAgentServiceImpl(OccupationService occupationService, CareerAiService careerAiService) {
        this.occupationService = occupationService;
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

        List<Occupation> matches = occupationService.matchOccupations(safeMajors, safeSkills, safePersonalities);

        return careerAiService.suggestCareer(
                formatList(safeMajors),
                formatList(safeSkills),
                formatList(safePersonalities),
                formatOccupations(matches)
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

    private String formatOccupations(List<Occupation> occupations) {
        if (occupations == null || occupations.isEmpty()) {
            return "暂无数据库匹配结果，可基于用户画像给出合理建议";
        }
        return occupations.stream()
                .limit(5)
                .map(this::formatOccupation)
                .collect(Collectors.joining("；"));
    }

    private String formatOccupation(Occupation occupation) {
        String name = occupation.getName() == null ? "未命名职业" : occupation.getName();
        String reason = occupation.getReason() == null ? "暂无推荐理由" : occupation.getReason();
        return name + "（" + reason + "）";
    }
}
