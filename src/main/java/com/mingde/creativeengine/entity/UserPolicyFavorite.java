package com.mingde.creativeengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_policy_favorite")
public class UserPolicyFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long policyId;
    private String policyName;
    private String policySnapshot;
    private LocalDateTime createTime;
}
