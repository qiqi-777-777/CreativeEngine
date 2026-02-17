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
     * 根据条件匹配职业
     */
    List<Occupation> matchOccupations(List<String> majors, List<String> skills, List<String> personalities);

}
