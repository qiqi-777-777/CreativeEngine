package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.observability.AgentTelemetry;
import com.mingde.creativeengine.observability.TraceContext;
import com.mingde.creativeengine.security.UserContext;
import com.mingde.creativeengine.service.agent.AgentLogService;
import com.mingde.creativeengine.service.agent.BpAgentService;
import com.mingde.creativeengine.service.agent.CareerAgentService;
import com.mingde.creativeengine.service.agent.PolicyAgentService;
import com.mingde.creativeengine.service.agent.RouterAgentService;
import com.mingde.creativeengine.service.agent.StartupAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified entry for lightweight scene agents.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private PolicyAgentService policyAgentService;

    @Autowired
    private CareerAgentService careerAgentService;

    @Autowired
    private BpAgentService bpAgentService;

    @Autowired
    private StartupAgentService startupAgentService;

    @Autowired
    private RouterAgentService routerAgentService;

    @Autowired
    private AgentLogService agentLogService;

    @Autowired
    private AgentTelemetry agentTelemetry;

    @PostMapping("/policy/ask")
    public Result<Map<String, Object>> askPolicy(@RequestBody Map<String, Object> request) {
        Long policyId = getLong(request, "policyId");
        String question = getString(request, "question");

        if (policyId == null || isBlank(question)) {
            return Result.error(ResultCode.PARAM_MISSING.getCode(), "policyId and question are required");
        }

        String answer = policyAgentService.askPolicy(policyId, question);
        agentLogService.saveLog(UserContext.getUserId(), "policy", question, answer, String.valueOf(policyId));

        Map<String, Object> data = new HashMap<>();
        data.put("answer", answer);
        return Result.success(withTraceId(data));
    }

    @PostMapping("/career/suggest")
    public Result<Map<String, Object>> suggestCareer(@RequestBody Map<String, Object> request) {
        List<String> majors = getStringList(request, "majors");
        List<String> skills = getStringList(request, "skills");
        List<String> personalities = getStringList(request, "personalities");

        if (majors.isEmpty() && skills.isEmpty() && personalities.isEmpty()) {
            return Result.error(ResultCode.PARAM_MISSING.getCode(), "majors, skills or personalities are required");
        }

        String suggestion = careerAgentService.suggestCareer(majors, skills, personalities);
        agentLogService.saveLog(UserContext.getUserId(), "career", request.toString(), suggestion, "");

        Map<String, Object> data = new HashMap<>();
        data.put("summary", suggestion);
        data.put("suggestion", suggestion);
        return Result.success(withTraceId(data));
    }

    @PostMapping("/bp/generate")
    public Result<Map<String, Object>> generateBp(@RequestBody Map<String, Object> request) {
        if (request == null || request.isEmpty()) {
            return Result.error(ResultCode.PARAM_MISSING.getCode(), "project information is required");
        }

        String content = bpAgentService.generateBp(request);
        agentLogService.saveLog(UserContext.getUserId(), "bp", request.toString(), content, getString(request, "projectName"));

        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        return Result.success(withTraceId(data));
    }

    @PostMapping("/startup/chat")
    public Result<Map<String, Object>> chatStartup(@RequestBody Map<String, Object> request) {
        String question = getQuestion(request);
        if (isBlank(question)) {
            return Result.error(ResultCode.PARAM_MISSING.getCode(), "question is required");
        }

        // 带 sessionId 时由 LangChain4j 的 ChatMemory 维护多轮上下文；
        // 未带时退化为「按请求传入历史」的无状态模式
        String sessionId = getString(request, "sessionId");
        String answer = isBlank(sessionId)
                ? startupAgentService.chatWithHistory(question, getHistory(request))
                : startupAgentService.chatWithSession(sessionId, question, getHistory(request));
        agentLogService.saveLog(UserContext.getUserId(), "startup", question, answer, sessionId);

        Map<String, Object> data = new HashMap<>();
        data.put("answer", answer);
        data.put("response", answer);
        return Result.success(withTraceId(data));
    }

    @PostMapping("/router/chat")
    public Result<Map<String, Object>> routeChat(@RequestBody Map<String, Object> request) {
        String question = getQuestion(request);
        if (isBlank(question)) {
            return Result.error(ResultCode.PARAM_MISSING.getCode(), "question is required");
        }

        Map<String, Object> data = routerAgentService.route(question, request);
        String route = getString(data, "route");
        String answer = getString(data, "answer");
        agentLogService.saveLog(UserContext.getUserId(), route.isEmpty() ? "router" : route, question, answer, "");
        return Result.success(withTraceId(data));
    }

    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> listLogs(@RequestParam(defaultValue = "30") int limit) {
        return Result.success(agentLogService.listRecentLogs(UserContext.getUserId(), limit));
    }

    /**
     * 模型与工具调用的运行指标快照。
     *
     * <p>完整指标在 {@code /actuator/prometheus}，这里给一个便于人工核对的精简视图，
     * 例如上线后确认"模型调用成功率、平均耗时、工具调用次数"是否符合预期。</p>
     */
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        return Result.success(agentTelemetry.snapshot());
    }

    /**
     * 把 traceId 放进响应，用户报错时可以直接提供该编号，便于在日志里定位整条链路。
     */
    private Map<String, Object> withTraceId(Map<String, Object> data) {
        data.put("traceId", TraceContext.current());
        return data;
    }

    private String getQuestion(Map<String, Object> request) {
        String question = getString(request, "question");
        if (isBlank(question)) {
            question = getString(request, "message");
        }
        return question;
    }

    private Long getLong(Map<String, Object> request, String key) {
        if (request == null) {
            return null;
        }
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getString(Map<String, Object> request, String key) {
        if (request == null) {
            return "";
        }
        Object value = request.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<String> getStringList(Map<String, Object> request, String key) {
        List<String> result = new ArrayList<>();
        if (request == null) {
            return result;
        }

        Object value = request.get(key);
        if (value == null) {
            return result;
        }

        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    result.add(item.toString().trim());
                }
            }
            return result;
        }

        for (String item : value.toString().split("[,，、;；\\s]+")) {
            if (!item.trim().isEmpty()) {
                result.add(item.trim());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getHistory(Map<String, Object> request) {
        if (request == null) {
            return new ArrayList<>();
        }
        Object value = request.get("history");
        if (!(value instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                history.add((Map<String, Object>) item);
            }
        }
        return history;
    }
}
