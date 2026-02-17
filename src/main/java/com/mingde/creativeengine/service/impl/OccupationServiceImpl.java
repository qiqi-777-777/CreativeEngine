package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.mapper.OccupationMapper;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.OccupationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职业服务实现类
 *
 * @author CreativeEngine Team
 */
@Service
public class OccupationServiceImpl implements OccupationService {

    private static final Logger log = LoggerFactory.getLogger(OccupationServiceImpl.class);

    @Autowired
    private OccupationMapper occupationMapper;

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public List<String> getAllMajors() {
        return occupationMapper.getAllMajors();
    }

    @Override
    public List<String> getAllSkills() {
        return occupationMapper.getAllSkills();
    }

    @Override
    public List<String> getAllPersonalities() {
        return occupationMapper.getAllPersonalities();
    }

    @Override
    public List<Occupation> matchOccupations(List<String> majors, List<String> skills, List<String> personalities) {
        log.info("开始匹配职业 - 专业: {}, 技能: {}, 性格: {}", majors, skills, personalities);

        // 如果没有任何筛选条件，返回空列表
        if ((majors == null || majors.isEmpty()) && 
            (skills == null || skills.isEmpty()) && 
            (personalities == null || personalities.isEmpty())) {
            return new ArrayList<>();
        }

        // 1. 从数据库匹配职业
        List<Occupation> dbMatches = matchFromDatabase(majors, skills, personalities);
        log.info("数据库匹配到 {} 个职业", dbMatches.size());

        // 2. 如果数据库匹配到3个或以上，直接返回前3个
        if (dbMatches.size() >= 3) {
            return dbMatches.subList(0, 3);
        }

        // 3. 如果不足3个，使用AI生成剩余的
        int needAiCount = 3 - dbMatches.size();
        log.info("需要AI生成 {} 个职业", needAiCount);

        List<Map<String, String>> aiRecommendations = deepSeekService.generateOccupationRecommendations(
                majors, skills, personalities, needAiCount);

        // 4. 合并数据库匹配和AI生成的结果
        List<Occupation> result = new ArrayList<>(dbMatches);
        
        for (Map<String, String> aiJob : aiRecommendations) {
            Occupation aiOccupation = new Occupation();
            aiOccupation.setName(aiJob.get("name"));
            aiOccupation.setReason(aiJob.get("reason"));
            aiOccupation.setMajors(majors != null ? new ArrayList<>(majors) : new ArrayList<>());
            aiOccupation.setSkills(skills != null ? new ArrayList<>(skills) : new ArrayList<>());
            aiOccupation.setPersonalityCode(personalities != null && !personalities.isEmpty() 
                    ? personalities.get(0) : "通用");
            aiOccupation.setIndustryHeat("B");
            result.add(aiOccupation);
        }

        log.info("最终返回 {} 个职业推荐", result.size());
        return result;
    }

    /**
     * 从数据库匹配职业
     */
    private List<Occupation> matchFromDatabase(List<String> majors, List<String> skills, List<String> personalities) {
        // 获取所有职业
        QueryWrapper<Occupation> queryWrapper = new QueryWrapper<>();
        List<Occupation> allOccupations = occupationMapper.selectList(queryWrapper);

        // 计算每个职业的匹配分数
        List<OccupationWithScore> occupationsWithScore = new ArrayList<>();
        
        for (Occupation occupation : allOccupations) {
            int score = calculateMatchScore(occupation, majors, skills, personalities);
            if (score > 0) {
                occupationsWithScore.add(new OccupationWithScore(occupation, score));
            }
        }

        // 按分数降序排序
        occupationsWithScore.sort((a, b) -> Integer.compare(b.score, a.score));

        // 返回排序后的职业列表
        return occupationsWithScore.stream()
                .map(ows -> ows.occupation)
                .collect(Collectors.toList());
    }

    /**
     * 计算职业匹配分数
     */
    private int calculateMatchScore(Occupation occupation, List<String> majors, 
                                    List<String> skills, List<String> personalities) {
        int score = 0;

        // 专业匹配（每匹配一个专业 +10分）
        if (majors != null && !majors.isEmpty() && occupation.getMajors() != null) {
            for (String major : majors) {
                if (occupation.getMajors().contains(major)) {
                    score += 10;
                }
            }
        }

        // 技术匹配（每匹配一个技术 +5分）
        if (skills != null && !skills.isEmpty() && occupation.getSkills() != null) {
            for (String skill : skills) {
                if (occupation.getSkills().contains(skill)) {
                    score += 5;
                }
            }
        }

        // 性格匹配（匹配 +20分）
        if (personalities != null && !personalities.isEmpty()) {
            if (personalities.contains(occupation.getPersonalityCode())) {
                score += 20;
            }
        }

        return score;
    }

    /**
     * 内部类：带分数的职业
     */
    private static class OccupationWithScore {
        Occupation occupation;
        int score;

        OccupationWithScore(Occupation occupation, int score) {
            this.occupation = occupation;
            this.score = score;
        }
    }

}
