package com.mingde.creativeengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 职业实体
 *
 * @author CreativeEngine Team
 */
@TableName(value = "occupation", autoResultMap = true)
public class Occupation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 职业ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 职业名称
     */
    private String name;

    /**
     * 匹配专业（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> majors;

    /**
     * 技术要求（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skills;

    /**
     * 行业热度
     */
    private String industryHeat;

    /**
     * 适合性格
     */
    private String personalityCode;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // Getter and Setter methods
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMajors() {
        return majors;
    }

    public void setMajors(List<String> majors) {
        this.majors = majors;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getIndustryHeat() {
        return industryHeat;
    }

    public void setIndustryHeat(String industryHeat) {
        this.industryHeat = industryHeat;
    }

    public String getPersonalityCode() {
        return personalityCode;
    }

    public void setPersonalityCode(String personalityCode) {
        this.personalityCode = personalityCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
