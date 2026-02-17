package com.mingde.creativeengine.service;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI服务接口
 *
 * @author CreativeEngine Team
 */
public interface DeepSeekService {

    /**
     * 根据用户选择生成职业推荐
     *
     * @param majors 专业列表
     * @param skills 技能列表
     * @param personalities 性格列表
     * @param count 需要生成的职业数量
     * @return 职业推荐列表，每个元素包含name和reason
     */
    List<Map<String, String>> generateOccupationRecommendations(
            List<String> majors,
            List<String> skills,
            List<String> personalities,
            int count
    );
    
    /**
     * 通用对话接口
     *
     * @param prompt 提示词
     * @return AI回复内容
     */
    String chat(String prompt);
}
