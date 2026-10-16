package com.mingde.creativeengine.service.agent.aiservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.observability.AgentTelemetry;
import com.mingde.creativeengine.rag.KeywordPolicyRetriever;
import com.mingde.creativeengine.service.OccupationService;
import com.mingde.creativeengine.service.PolicyDataService;
import com.mingde.creativeengine.service.agent.tool.OccupationTools;
import com.mingde.creativeengine.service.agent.tool.PolicyTools;
import com.mingde.creativeengine.service.impl.DeepSeekServiceImpl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LangChain4j 装配测试：不启动 Spring、不访问网络。
 *
 * <p>用假的 ChatModel 记录实际发给模型的消息，验证注解里的提示词模板、@V 占位符绑定、
 * ChatMemory 会话记忆、JSON 输出解析与异常降级是否符合预期。</p>
 */
class LangChain4jAgentWiringTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AgentTelemetry telemetry = new AgentTelemetry(meterRegistry);

    /** 指标名 → 计数，便于断言"这次调用真的被记录了"。 */
    private double timerCount(String name) {
        return meterRegistry.find(name).timers().stream().mapToDouble(Timer::count).sum();
    }

    /**
     * 记录请求内容的假模型，用于断言真正发给模型的消息。
     */
    private static class RecordingChatModel implements ChatModel {

        private final List<ChatRequest> requests = new ArrayList<>();
        private final List<ChatResponse> queuedReplies = new ArrayList<>();
        private String reply = "stub-reply";
        private RuntimeException failure;

        @Override
        public ChatResponse chat(ChatRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
            }
            if (!queuedReplies.isEmpty()) {
                return queuedReplies.remove(0);
            }
            return ChatResponse.builder().aiMessage(AiMessage.from(reply)).build();
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            return chat(request);
        }

        void setReply(String reply) {
            this.reply = reply;
        }

        void setFailure(RuntimeException failure) {
            this.failure = failure;
        }

        /**
         * 按顺序返回多轮响应，用于模拟"先要求调用工具、再给出最终回答"的模型行为。
         */
        void queueReplies(ChatResponse... replies) {
            this.queuedReplies.addAll(List.of(replies));
        }

        ChatRequest lastRequest() {
            assertFalse(requests.isEmpty(), "模型没有被调用");
            return requests.get(requests.size() - 1);
        }

        ChatRequest requestAt(int index) {
            return requests.get(index);
        }

        int callCount() {
            return requests.size();
        }

        String lastCallText() {
            assertFalse(requests.isEmpty(), "模型没有被调用");
            return textOf(lastRequest());
        }

        String textOf(ChatRequest request) {
            StringBuilder text = new StringBuilder();
            for (ChatMessage message : request.messages()) {
                text.append(LangChain4jAgentWiringTest.textOf(message)).append("\n");
            }
            return text.toString();
        }

        List<String> lastToolNames() {
            List<String> names = new ArrayList<>();
            for (ToolSpecification specification : lastRequest().toolSpecifications()) {
                names.add(specification.name());
            }
            return names;
        }
    }

    private static String textOf(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        }
        if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        }
        if (message instanceof ToolExecutionResultMessage) {
            return ((ToolExecutionResultMessage) message).text();
        }
        return String.valueOf(message);
    }

    @Test
    void policyAgentInjectsBusinessDataIntoDeclarativePrompt() {
        RecordingChatModel model = new RecordingChatModel();
        PolicyAiService service = AgentAiServiceFactory.policyAiService(model, policyTools(), telemetry);

        service.askByPolicy(
                "大学生创业补贴政策",
                "补贴、创业",
                "符合条件的毕业生可申请一次性创业补贴",
                "我符合条件吗"
        );

        String sent = model.lastCallText();
        assertTrue(sent.contains("政策解读助手"), sent);
        assertTrue(sent.contains("大学生创业补贴政策"), sent);
        assertTrue(sent.contains("符合条件的毕业生可申请一次性创业补贴"), sent);
        assertTrue(sent.contains("我符合条件吗"), sent);
        assertTrue(sent.contains("申报建议"), sent);
    }

    @Test
    void policyAgentRegistersToolsForQuestionOnlyFlow() {
        RecordingChatModel model = new RecordingChatModel();
        PolicyAiService service = AgentAiServiceFactory.policyAiService(model, policyTools(), telemetry);

        service.askWithTools("大学生创业补贴怎么申请");

        List<String> toolNames = model.lastToolNames();
        assertTrue(toolNames.contains("searchPolicies"), "工具未注册到模型请求：" + toolNames);
        assertTrue(toolNames.contains("getPolicyDetail"), "工具未注册到模型请求：" + toolNames);
        assertTrue(model.lastCallText().contains("先调用工具查证"), model.lastCallText());
    }

    @Test
    void toolResultIsFedBackToModelSoItCanAnswerFromRealData() {
        RecordingChatModel model = new RecordingChatModel();
        // 第一轮：模型要求检索政策；第二轮：模型基于工具结果给出回答
        model.queueReplies(
                ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                        .id("call-1").name("searchPolicies").arguments("{\"keyword\":\"创业补贴\"}").build())).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("可以申请一次性创业补贴，具体条件以官方通知为准。")).build());

        PolicyAiService service = AgentAiServiceFactory.policyAiService(model, policyTools(), telemetry);

        String answer = service.askWithTools("大学生创业补贴怎么申请");

        assertEquals(2, model.callCount(), "应该发生两轮模型调用：先决定工具、再生成回答");
        assertEquals("可以申请一次性创业补贴，具体条件以官方通知为准。", answer);

        String secondTurn = model.textOf(model.requestAt(1));
        assertTrue(secondTurn.contains("大学生创业补贴政策"),
                "工具检索到的政策应作为工具消息回灌给模型：\n" + secondTurn);

        // 工具真的被执行了，就应该留下一条工具调用指标（F：可观测）
        assertEquals(1.0, timerCount("agent.tool.call"), 0.0001,
                "工具调用应被记为 agent.tool.call 指标");
    }

    @Test
    void careerAgentRegistersOccupationToolsInsteadOfInjectingMatches() {
        RecordingChatModel model = new RecordingChatModel();
        CareerAiService service = AgentAiServiceFactory.careerAiService(model, occupationTools(), telemetry);

        service.suggestCareer("计算机科学与技术", "Java", "研究型");

        List<String> toolNames = model.lastToolNames();
        assertTrue(toolNames.contains("matchOccupations"), "工具未注册到模型请求：" + toolNames);
        assertTrue(toolNames.contains("getOccupationTags"), "工具未注册到模型请求：" + toolNames);

        String sent = model.lastCallText();
        assertTrue(sent.contains("计算机科学与技术"), sent);
        assertTrue(sent.contains("能力提升建议"), sent);
    }

    @Test
    void bpAgentInjectsProjectBriefAndRequirement() {
        RecordingChatModel model = new RecordingChatModel();
        BpAiService service = AgentAiServiceFactory.bpAiService(model);

        service.generateBp("项目名称：校园二手交易平台", "重点写商业模式");

        String sent = model.lastCallText();
        assertTrue(sent.contains("校园二手交易平台"), sent);
        assertTrue(sent.contains("重点写商业模式"), sent);
        assertTrue(sent.contains("发展规划"), sent);
    }

    @Test
    void startupAgentKeepsMultiTurnContextWithinOneSessionOnly() {
        RecordingChatModel model = new RecordingChatModel();
        InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();
        StartupAiService service = AgentAiServiceFactory.startupAiService(model, memoryStore);

        model.setReply("第一步先做需求验证");
        service.chat("session-a", "我想做校园创业项目，第一步做什么？");

        model.setReply("第二步做原型");
        service.chat("session-a", "那第二步呢？");

        String secondTurn = model.lastCallText();
        assertTrue(secondTurn.contains("我想做校园创业项目，第一步做什么？"),
                "同一会话应带上上一轮用户消息：\n" + secondTurn);
        assertTrue(secondTurn.contains("第一步先做需求验证"),
                "同一会话应带上上一轮模型回复：\n" + secondTurn);

        model.setReply("好的");
        service.chat("session-b", "这是一个全新会话的问题");
        String otherSession = model.lastCallText();
        assertFalse(otherSession.contains("我想做校园创业项目，第一步做什么？"),
                "不同会话之间不应串上下文：\n" + otherSession);
    }

    @Test
    void occupationAgentAsksForJsonOutput() {
        RecordingChatModel model = new RecordingChatModel();
        OccupationAiService service = AgentAiServiceFactory.occupationAiService(model);

        service.recommendJson("计算机科学与技术", "Java、MySQL", "研究型", 3);

        String sent = model.lastCallText();
        // DeepSeek 的 json_object 模式要求提示词中出现 json 字样
        assertTrue(sent.contains("json"), sent);
        assertTrue(sent.contains("recommendations"), sent);
        assertTrue(sent.contains("计算机科学与技术"), sent);
    }

    @Test
    void occupationRecommendationsAreParsedFromWrappedJson() {
        RecordingChatModel model = new RecordingChatModel();
        model.setReply("{\"recommendations\":[{\"name\":\"后端开发工程师\",\"reason\":\"技能高度匹配\"},"
                + "{\"name\":\"数据分析师\",\"reason\":\"专业相关\"}]}");

        DeepSeekServiceImpl service = newService(model);

        List<Map<String, String>> result = service.generateOccupationRecommendations(
                List.of("计算机科学与技术"), List.of("Java"), List.of("研究型"), 2);

        assertEquals(2, result.size());
        assertEquals("后端开发工程师", result.get(0).get("name"));
        assertEquals("技能高度匹配", result.get(0).get("reason"));
        assertEquals("数据分析师", result.get(1).get("name"));
    }

    @Test
    void occupationRecommendationsTolerateBareArrayAndCodeFence() {
        RecordingChatModel model = new RecordingChatModel();
        model.setReply("```json\n[{\"name\":\"测试工程师\",\"reason\":\"细致耐心\"}]\n```");

        DeepSeekServiceImpl service = newService(model);

        List<Map<String, String>> result = service.generateOccupationRecommendations(
                List.of("计算机科学与技术"), List.of("测试"), List.of("严谨"), 1);

        assertEquals(1, result.size());
        assertEquals("测试工程师", result.get(0).get("name"));
    }

    @Test
    void modelFailureDegradesGracefullyInsteadOfThrowing() {
        RecordingChatModel model = new RecordingChatModel();
        model.setFailure(new IllegalStateException("network down"));

        DeepSeekServiceImpl service = newService(model);

        String answer = service.chat("你好");
        assertNotNull(answer);
        assertFalse(answer.contains("network down"), "不应把异常信息直接返回给前端: " + answer);
        assertTrue(answer.contains("稍后"), answer);

        List<Map<String, String>> recommendations = service.generateOccupationRecommendations(
                List.of("计算机"), List.of("Java"), List.of("研究型"), 2);
        assertEquals(2, recommendations.size());
        assertEquals("产品经理", recommendations.get(0).get("name"));
    }

    /**
     * 政策工具依赖的检索服务：返回一条固定政策，用于验证工具调用链路。
     */
    private PolicyTools policyTools() {
        PolicyDataService policyDataService = new PolicyDataService() {
            @Override
            public List<PolicyData> getAllPolicies() {
                return Collections.emptyList();
            }

            @Override
            public PolicyData getPolicyById(Long id) {
                return policy(9L);
            }

            @Override
            public PolicyData getPolicyByName(String policyName) {
                return null;
            }

            @Override
            public List<PolicyData> searchPolicies(String keyword) {
                return List.of(policy(9L));
            }
        };
        return new PolicyTools(policyDataService, new KeywordPolicyRetriever(policyDataService));
    }

    /**
     * 职业工具依赖的服务：只实现工具会用到的方法。
     */
    private OccupationTools occupationTools() {
        OccupationService occupationService = new OccupationService() {
            @Override
            public List<String> getAllMajors() {
                return List.of("计算机科学与技术");
            }

            @Override
            public List<String> getAllSkills() {
                return List.of("Java");
            }

            @Override
            public List<String> getAllPersonalities() {
                return List.of("研究型");
            }

            @Override
            public List<Occupation> matchOccupations(List<String> majors, List<String> skills, List<String> personalities) {
                return Collections.emptyList();
            }

            @Override
            public List<Occupation> matchFromDatabase(List<String> majors, List<String> skills, List<String> personalities) {
                return Collections.emptyList();
            }
        };
        return new OccupationTools(occupationService);
    }

    private static PolicyData policy(long id) {
        PolicyData policy = new PolicyData();
        policy.setId(id);
        policy.setPolicyName("大学生创业补贴政策");
        policy.setKeywords("补贴、创业");
        policy.setContent("符合条件的毕业生可申请一次性创业补贴");
        return policy;
    }

    private DeepSeekServiceImpl newService(RecordingChatModel model) {
        return new DeepSeekServiceImpl(model,
                AgentAiServiceFactory.occupationAiService(model),
                new ObjectMapper());
    }
}
