package com.mingde.creativeengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_career_record")
public class UserCareerRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String recordKey;
    private String resultData;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
