package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.OccupationService;
import com.mingde.creativeengine.service.agent.CareerAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Career planning agent based on occupation matching and DeepSeek.
 */
@Service
public class CareerAgentServiceImpl implements CareerAgentService {

    @Autowired
    private OccupationService occupationService;

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public String suggestCareer(List<String> majors, List<String> skills, List<String> personalities) {
        List<String> safeMajors = safeList(majors);
        List<String> safeSkills = safeList(skills);
        List<String> safePersonalities = safeList(personalities);

        if (safeMajors.isEmpty() && safeSkills.isEmpty() && safePersonalities.isEmpty()) {
            return "请先选择专业、技能或性格标签，系统才能生成职业规划建议。";
        }

        List<Occupation> matches = occupationService.matchOccupations(safeMajors, safeSkills, safePersonalities);
        String prompt = buildPrompt(safeMajors, safeSkills, safePersonalities, matches);
        return deepSeekService.chat(prompt);
    }

    private String buildPrompt(
            List<String> majors,
            List<String> skills,
            List<String> personalities,
            List<Occupation> matches
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个面向大学生和青年创业者的职业规划顾问。\n");
        prompt.append("请根据用户的专业、技能和性格特征，给出清晰、具体、可执行的职业方向建议。\n\n");
        prompt.append("专业：").append(formatList(majors)).append("\n");
        prompt.append("技能：").append(formatList(skills)).append("\n");
        prompt.append("性格：").append(formatList(personalities)).append("\n");
        prompt.append("数据库初步匹配结果：").append(formatOccupations(matches)).append("\n\n");
        prompt.append("请按照以下结构回答：\n");
        prompt.append("1. 综合分析\n");
        prompt.append("2. 推荐职业方向\n");
        prompt.append("3. 匹配原因\n");
        prompt.append("4. 能力提升建议\n");
        prompt.append("5. 可延伸的创业方向\n");
        return prompt.toString();
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
