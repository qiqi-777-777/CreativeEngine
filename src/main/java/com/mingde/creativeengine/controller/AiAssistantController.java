package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI助手控制器
 *
 * @author CreativeEngine Team
 */
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
    
    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);
    
    @Autowired
    private DeepSeekService deepSeekService;
    

    private static final Map<String, List<Map<String, Object>>> sessionHistories = new ConcurrentHashMap<>();
    private static final Map<String, Long> sessionLastAccessTime = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; 
    
    /**
     * 创建新会话
     */
    @PostMapping("/session/create")
    public Map<String, Object> createSession() {
        Map<String, Object> result = new HashMap<>();
        try {
            String sessionId = UUID.randomUUID().toString();
            sessionHistories.put(sessionId, new ArrayList<>());
            sessionLastAccessTime.put(sessionId, System.currentTimeMillis());
            
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);
            
            log.info("创建新会话: {}", sessionId);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            result.put("code", 500);
            result.put("message", "创建会话失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * AI对话
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sessionId = (String) params.get("sessionId");
            String userMessage = (String) params.get("message");
            
            if (sessionId == null || sessionId.isEmpty()) {
                result.put("code", 400);
                result.put("message", "会话ID不能为空");
                return result;
            }
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "消息不能为空");
                return result;
            }
            
            // 检查会话是否存在
            if (!sessionHistories.containsKey(sessionId)) {
                result.put("code", 404);
                result.put("message", "会话不存在或已过期");
                return result;
            }
            
            // 清理过期会话
            cleanExpiredSessions();
            
            // 更新最后访问时间
            sessionLastAccessTime.put(sessionId, System.currentTimeMillis());
            
            // 获取会话历史
            List<Map<String, Object>> history = sessionHistories.get(sessionId);
            
            // 添加用户消息到历史
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("id", System.currentTimeMillis());
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            userMsg.put("createTime", new Date());
            history.add(userMsg);
            
            // 构建完整的对话上下文
            StringBuilder contextPrompt = new StringBuilder();
            contextPrompt.append("你是创享引擎的AI助手，专门帮助创业者解答创业相关问题。\n\n");
            
            // 添加历史对话（最近5轮）
            int startIndex = Math.max(0, history.size() - 10);
            for (int i = startIndex; i < history.size() - 1; i++) {
                Map<String, Object> msg = history.get(i);
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                if ("user".equals(role)) {
                    contextPrompt.append("用户: ").append(content).append("\n");
                } else if ("assistant".equals(role)) {
                    contextPrompt.append("助手: ").append(content).append("\n");
                }
            }
            
            // 添加当前问题
            contextPrompt.append("\n当前问题: ").append(userMessage);
            contextPrompt.append("\n\n请简洁专业地回答用户的问题。");
            
            // 调用DeepSeek API
            String aiResponse = deepSeekService.chat(contextPrompt.toString());
            
            // 添加AI回复到历史
            Map<String, Object> aiMsg = new HashMap<>();
            aiMsg.put("id", System.currentTimeMillis() + 1);
            aiMsg.put("role", "assistant");
            aiMsg.put("content", aiResponse);
            aiMsg.put("createTime", new Date());
            history.add(aiMsg);
            
            // 返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("response", aiResponse);
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);
            
            log.info("AI对话 - 会话: {}, 用户消息: {}, AI回复长度: {}", 
                    sessionId, userMessage.substring(0, Math.min(50, userMessage.length())), aiResponse.length());
            
        } catch (Exception e) {
            log.error("AI对话失败", e);
            result.put("code", 500);
            result.put("message", "对话失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取会话历史
     */
    @GetMapping("/history")
    public Map<String, Object> getHistory(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!sessionHistories.containsKey(sessionId)) {
                result.put("code", 404);
                result.put("message", "会话不存在或已过期");
                return result;
            }
            
            List<Map<String, Object>> history = sessionHistories.get(sessionId);
            
            // 返回最近的limit条记录
            int startIndex = Math.max(0, history.size() - limit);
            List<Map<String, Object>> limitedHistory = new ArrayList<>(
                    history.subList(startIndex, history.size())
            );
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", limitedHistory);
            
        } catch (Exception e) {
            log.error("获取会话历史失败", e);
            result.put("code", 500);
            result.put("message", "获取历史失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取所有会话列表
     */
    @GetMapping("/sessions")
    public Map<String, Object> getSessions() {
        Map<String, Object> result = new HashMap<>();
        try {
            cleanExpiredSessions();
            
            List<Map<String, Object>> sessions = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : sessionHistories.entrySet()) {
                String sessionId = entry.getKey();
                List<Map<String, Object>> history = entry.getValue();
                
                if (!history.isEmpty()) {
                    Map<String, Object> session = new HashMap<>();
                    session.put("sessionId", sessionId);
                    session.put("messageCount", history.size());
                    
                    // 获取第一条用户消息作为会话标题
                    String title = "新对话";
                    for (Map<String, Object> msg : history) {
                        if ("user".equals(msg.get("role"))) {
                            String content = (String) msg.get("content");
                            title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                            break;
                        }
                    }
                    session.put("title", title);
                    session.put("lastAccessTime", sessionLastAccessTime.get(sessionId));
                    
                    sessions.add(session);
                }
            }
            
            // 按最后访问时间倒序排序
            sessions.sort((s1, s2) -> {
                Long t1 = (Long) s1.get("lastAccessTime");
                Long t2 = (Long) s2.get("lastAccessTime");
                return t2.compareTo(t1);
            });
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", sessions);
            
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            result.put("code", 500);
            result.put("message", "获取会话列表失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 清理过期会话
     */
    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        List<String> expiredSessions = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : sessionLastAccessTime.entrySet()) {
            if (now - entry.getValue() > SESSION_TIMEOUT) {
                expiredSessions.add(entry.getKey());
            }
        }
        
        for (String sessionId : expiredSessions) {
            sessionHistories.remove(sessionId);
            sessionLastAccessTime.remove(sessionId);
            log.info("清理过期会话: {}", sessionId);
        }
    }
}
