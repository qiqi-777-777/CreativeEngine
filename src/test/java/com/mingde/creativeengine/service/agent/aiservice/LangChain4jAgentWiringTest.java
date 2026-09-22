package com.mingde.creativeengine.service.agent.aiservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.service.impl.DeepSeekServiceImpl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

    /**
     * 记录请求内容的假模型，用于断言真正发给模型的消息。
     */
    private static class RecordingChatModel implements ChatModel {

        private final List<ChatRequest> requests = new ArrayList<>();
        private String reply = "stub-reply";
        private RuntimeException failure;

        @Override
        public ChatResponse chat(ChatRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
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

        String lastCallText() {
            assertFalse(requests.isEmpty(), "模型没有被调用");
            StringBuilder text = new StringBuilder();
            for (ChatMessage message : requests.get(requests.size() - 1).messages()) {
                text.append(textOf(message)).append("\n");
            }
            return text.toString();
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
        return String.valueOf(message);
    }

    @Test
    void policyAgentInjectsBusinessDataIntoDeclarativePrompt() {
        RecordingChatModel model = new RecordingChatModel();
        PolicyAiService service = AgentAiServiceFactory.policyAiService(model);

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
    void careerAgentInjectsProfileLabelsAndDbMatches() {
        RecordingChatModel model = new RecordingChatModel();
        CareerAiService service = AgentAiServiceFactory.careerAiService(model);

        service.suggestCareer("计算机科学与技术", "Java", "研究型", "后端开发工程师（技能匹配）");

        String sent = model.lastCallText();
        assertTrue(sent.contains("计算机科学与技术"), sent);
        assertTrue(sent.contains("后端开发工程师（技能匹配）"), sent);
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

    private DeepSeekServiceImpl newService(RecordingChatModel model) {
        return new DeepSeekServiceImpl(model,
                AgentAiServiceFactory.occupationAiService(model),
                new ObjectMapper());
    }
}
