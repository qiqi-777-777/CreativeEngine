package com.mingde.creativeengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_session_log")
public class AgentSessionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String agentType;
    private String inputText;
    private String outputText;
    private String relatedId;
    private LocalDateTime createTime;
}
