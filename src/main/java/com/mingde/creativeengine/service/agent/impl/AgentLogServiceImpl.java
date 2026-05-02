package com.mingde.creativeengine.service.agent.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mingde.creativeengine.entity.AgentSessionLog;
import com.mingde.creativeengine.mapper.AgentSessionLogMapper;
import com.mingde.creativeengine.service.agent.AgentLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent log persistence. Write failures are swallowed so AI answers are not
 * blocked when the optional log table has not been imported yet.
 */
@Service
public class AgentLogServiceImpl implements AgentLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentLogServiceImpl.class);
    private static final int MAX_TEXT_LENGTH = 8000;

    @Autowired
    private AgentSessionLogMapper agentSessionLogMapper;

    @Override
    public void saveLog(Long userId, String agentType, String inputText, String outputText, String relatedId) {
        try {
            AgentSessionLog record = new AgentSessionLog();
            record.setUserId(userId);
            record.setAgentType(trim(agentType, 40));
            record.setInputText(trim(inputText, MAX_TEXT_LENGTH));
            record.setOutputText(trim(outputText, MAX_TEXT_LENGTH));
            record.setRelatedId(trim(relatedId, 64));
            record.setCreateTime(LocalDateTime.now());
            agentSessionLogMapper.insert(record);
        } catch (Exception e) {
            log.warn("Failed to save agent log. The optional agent_session_log table may be missing.", e);
        }
    }

    @Override
    public List<Map<String, Object>> listRecentLogs(Long userId, int limit) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            int safeLimit = Math.max(1, Math.min(limit, 100));
            QueryWrapper<AgentSessionLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                    .orderByDesc("create_time")
                    .orderByDesc("id")
                    .last("LIMIT " + safeLimit);

            List<Map<String, Object>> result = new ArrayList<>();
            for (AgentSessionLog item : agentSessionLogMapper.selectList(wrapper)) {
                result.add(toMap(item));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to list agent logs. The optional agent_session_log table may be missing.", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> toMap(AgentSessionLog item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", item.getId());
        data.put("userId", item.getUserId());
        data.put("agentType", item.getAgentType());
        data.put("inputText", item.getInputText());
        data.put("outputText", item.getOutputText());
        data.put("relatedId", item.getRelatedId());
        data.put("createdAt", item.getCreateTime() == null ? "" : item.getCreateTime().toString());
        return data;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
