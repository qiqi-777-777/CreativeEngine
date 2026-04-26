package com.mingde.creativeengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_bp_draft")
public class UserBpDraft {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String draftKey;
    private String projectName;
    private String oneLiner;
    private String formData;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
