package com.mingde.creativeengine.service;

import com.mingde.creativeengine.entity.Occupation;

import java.util.List;

/**
 * 职业服务接口
 *
 * @author CreativeEngine Team
 */
public interface OccupationService {

    /**
     * 获取所有专业列表
     */
    List<String> getAllMajors();

    /**
     * 获取所有技术列表
     */
    List<String> getAllSkills();

    /**
     * 获取所有性格类型列表
     */
    List<String> getAllPersonalities();

    /**
     * 根据条件匹配职业（结果不足 3 条时会调用大模型补全）
     */
    List<Occupation> matchOccupations(List<String> majors, List<String> skills, List<String> personalities);

    /**
     * 仅按标签做数据库匹配，不调用任何大模型。
     *
     * <p>供 Agent 工具使用：工具内部不能再发起模型调用，否则一次问答会变成嵌套的多次模型往返。
     * 需要"不足 3 条时用 AI 补全"的场景请使用 {@link #matchOccupations}。</p>
     */
    List<Occupation> matchFromDatabase(List<String> majors, List<String> skills, List<String> personalities);

}
