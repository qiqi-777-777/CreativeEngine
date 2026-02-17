package com.mingde.creativeengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mingde.creativeengine.entity.Occupation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职业Mapper
 *
 * @author CreativeEngine Team
 */
@Mapper
public interface OccupationMapper extends BaseMapper<Occupation> {

    /**
     * 获取所有专业列表（去重）
     */
    @Select("SELECT DISTINCT JSON_UNQUOTE(JSON_EXTRACT(majors, CONCAT('$[', idx, ']'))) AS major " +
            "FROM occupation " +
            "CROSS JOIN (SELECT 0 AS idx UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 " +
            "UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) AS indices " +
            "WHERE JSON_LENGTH(majors) > idx " +
            "AND JSON_EXTRACT(majors, CONCAT('$[', idx, ']')) IS NOT NULL " +
            "ORDER BY major")
    List<String> getAllMajors();

    /**
     * 获取所有技术列表（去重）
     */
    @Select("SELECT DISTINCT JSON_UNQUOTE(JSON_EXTRACT(skills, CONCAT('$[', idx, ']'))) AS skill " +
            "FROM occupation " +
            "CROSS JOIN (SELECT 0 AS idx UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 " +
            "UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 " +
            "UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11) AS indices " +
            "WHERE JSON_LENGTH(skills) > idx " +
            "AND JSON_EXTRACT(skills, CONCAT('$[', idx, ']')) IS NOT NULL " +
            "ORDER BY skill")
    List<String> getAllSkills();

    /**
     * 获取所有性格类型列表（去重）
     */
    @Select("SELECT DISTINCT personality_code FROM occupation ORDER BY personality_code")
    List<String> getAllPersonalities();

}
