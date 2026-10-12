package com.mingde.creativeengine.service.agent.tool;

import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.service.OccupationService;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 职业规划 Agent 可调用的工具集。
 *
 * <p>改造前是 Java 侧先跑一遍匹配、再把结果塞进提示词；现在由模型决定用什么标签查、
 * 要不要先看一遍可用标签，把"用户口语化描述 → 库内标准标签"这一步也交给模型完成。</p>
 *
 * <p>注意这里只调用<b>纯数据库匹配</b>（{@code matchFromDatabase}）：原
 * {@code matchOccupations} 在结果不足 3 条时还会再调一次大模型补全，如果直接把它做成工具，
 * 就会形成"工具里嵌套模型调用"，一次问答的模型往返次数和延迟都不可控。</p>
 */
@Slf4j
@Component
public class OccupationTools {

    /** 匹配结果最多返回几条。 */
    private static final int MAX_MATCH_RESULT = 5;

    /** 标签列表每类最多展示多少个，避免把整个标签库塞进上下文。 */
    private static final int MAX_TAG_DISPLAY = 40;

    private final OccupationService occupationService;

    public OccupationTools(OccupationService occupationService) {
        this.occupationService = occupationService;
    }

    @Tool("获取职业库支持的专业、技能、性格标签。用户用口语描述自己时，可先调用本工具把描述映射为标准标签再匹配。")
    public String getOccupationTags() {
        try {
            return "专业标签：" + ToolSupport.joinLimit(occupationService.getAllMajors(), MAX_TAG_DISPLAY)
                    + "\n技能标签：" + ToolSupport.joinLimit(occupationService.getAllSkills(), MAX_TAG_DISPLAY)
                    + "\n性格标签：" + ToolSupport.joinLimit(occupationService.getAllPersonalities(), MAX_TAG_DISPLAY);
        } catch (Exception e) {
            log.error("职业标签工具执行失败", e);
            return "职业标签查询暂时不可用（" + e.getClass().getSimpleName() + "）。"
                    + "请基于用户提供的描述给出通用建议，并说明未能对照职业库校验。";
        }
    }

    @Tool("按专业、技能、性格标签查询职业库匹配结果，返回职业名称与推荐理由。只返回职业库中的真实数据，不含推测。")
    public String matchOccupations(@P("专业标签，多个用顿号或逗号分隔，可为空") String majors,
                                   @P("技能标签，多个用顿号或逗号分隔，可为空") String skills,
                                   @P("性格标签，多个用顿号或逗号分隔，可为空") String personalities) {
        List<String> majorList = ToolSupport.splitTags(majors);
        List<String> skillList = ToolSupport.splitTags(skills);
        List<String> personalityList = ToolSupport.splitTags(personalities);

        if (majorList.isEmpty() && skillList.isEmpty() && personalityList.isEmpty()) {
            return "未收到任何可用标签，无法匹配。请先向用户确认专业、技能或性格，"
                    + "或调用 getOccupationTags 查看可用的标准标签。";
        }

        try {
            List<Occupation> matches = occupationService.matchFromDatabase(majorList, skillList, personalityList);
            if (matches == null || matches.isEmpty()) {
                return "职业库中没有匹配到符合条件的职业（专业=" + describe(majorList)
                        + "，技能=" + describe(skillList)
                        + "，性格=" + describe(personalityList) + "）。"
                        + "可以换用 getOccupationTags 中的标准标签再试一次，或减少限定条件。";
            }

            int count = Math.min(MAX_MATCH_RESULT, matches.size());
            StringBuilder result = new StringBuilder();
            result.append("职业库匹配到 ").append(matches.size()).append(" 个职业，以下为前 ").append(count).append(" 个：\n");
            for (int i = 0; i < count; i++) {
                Occupation occupation = matches.get(i);
                result.append(i + 1).append(". ")
                        .append(ToolSupport.defaultValue(occupation.getName()))
                        .append("：")
                        .append(ToolSupport.defaultValue(occupation.getReason()))
                        .append("\n");
            }
            return result.toString().trim();
        } catch (Exception e) {
            log.error("职业匹配工具执行失败，majors={}, skills={}, personalities={}", majors, skills, personalities, e);
            return "职业库查询暂时不可用（" + e.getClass().getSimpleName() + "）。"
                    + "请基于用户画像给出通用职业方向建议，并说明本次未能对照职业库数据。";
        }
    }

    private String describe(List<String> values) {
        return values.isEmpty() ? "未提供" : values.stream().collect(Collectors.joining("、"));
    }
}
