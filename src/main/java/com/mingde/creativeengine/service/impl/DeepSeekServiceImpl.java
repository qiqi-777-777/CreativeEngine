package com.mingde.creativeengine.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mingde.creativeengine.service.DeepSeekService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek AI服务实现
 *
 * @author CreativeEngine Team
 */
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekServiceImpl.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    @Override
    public List<Map<String, String>> generateOccupationRecommendations(
            List<String> majors,
            List<String> skills,
            List<String> personalities,
            int count) {

        try {
            String prompt = buildPrompt(majors, skills, personalities, count);
            String response = callDeepSeekAPI(prompt);
            return parseResponse(response, count);
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            return getDefaultRecommendations(count);
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(List<String> majors, List<String> skills, 
                               List<String> personalities, int count) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个职业规划专家。根据以下用户信息，推荐").append(count).append("个适合的职业。\n\n");

        if (majors != null && !majors.isEmpty()) {
            prompt.append("专业：").append(String.join("、", majors)).append("\n");
        }
        if (skills != null && !skills.isEmpty()) {
            prompt.append("技能：").append(String.join("、", skills)).append("\n");
        }
        if (personalities != null && !personalities.isEmpty()) {
            prompt.append("性格：").append(String.join("、", personalities)).append("\n");
        }

        prompt.append("\n请按以下JSON格式返回").append(count).append("个职业推荐：\n");
        prompt.append("[\n");
        prompt.append("  {\"name\": \"职业名称\", \"reason\": \"一句话推荐理由（30字以内）\"},\n");
        prompt.append("  ...\n");
        prompt.append("]\n\n");
        prompt.append("要求：\n");
        prompt.append("1. 只返回JSON数组，不要其他文字\n");
        prompt.append("2. 推荐理由简洁明了，突出匹配点\n");
        prompt.append("3. 职业要具体，不要太宽泛\n");

        return prompt.toString();
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekAPI(String prompt) throws Exception {
        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);

        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.7);

        // 发送请求
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("DeepSeek API返回错误: " + response.code());
            }

            String responseBody = response.body().string();
            log.info("DeepSeek API响应: {}", responseBody);

            // 解析响应
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject messageObj = firstChoice.getAsJsonObject("message");
                return messageObj.get("content").getAsString();
            }

            throw new Exception("DeepSeek API返回格式错误");
        }
    }

    /**
     * 解析AI响应
     */
    private List<Map<String, String>> parseResponse(String response, int count) {
        try {
            // 去除可能的markdown代码块标记
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();

            // 解析JSON
            JsonArray jsonArray = gson.fromJson(cleanedResponse, JsonArray.class);
            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 0; i < Math.min(jsonArray.size(), count); i++) {
                JsonObject job = jsonArray.get(i).getAsJsonObject();
                Map<String, String> jobMap = new HashMap<>();
                jobMap.put("name", job.get("name").getAsString());
                jobMap.put("reason", job.get("reason").getAsString());
                result.add(jobMap);
            }

            return result;
        } catch (Exception e) {
            log.error("解析DeepSeek响应失败", e);
            return getDefaultRecommendations(count);
        }
    }

    @Override
    public String chat(String prompt) {
        try {
            log.info("开始调用DeepSeek API，提示词长度: {}", prompt.length());
            
            // 构建请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            
            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            requestBody.add("messages", messages);
            
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2000);
            
            log.info("请求URL: {}", apiUrl);
            log.info("请求体: {}", requestBody.toString());
            
            // 发送请求
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                    .build();
            
            log.info("开始发送HTTP请求...");
            Response response = client.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                log.error("DeepSeek API返回错误状态码: {}", response.code());
                return "抱歉，AI服务暂时繁忙，请稍后再试。";
            }
            
            String responseBody = response.body().string();
            log.info("DeepSeek Chat响应状态码: {}, 响应长度: {}", response.code(), responseBody.length());
            
            // 解析响应
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // 检查是否有错误
            if (jsonResponse.has("error")) {
                JsonObject error = jsonResponse.getAsJsonObject("error");
                String errorMsg = error.get("message").getAsString();
                log.error("DeepSeek API返回错误: {}", errorMsg);
                return "抱歉，AI服务返回错误：" + errorMsg;
            }
            
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            
            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject messageObj = choice.getAsJsonObject("message");
                String content = messageObj.get("content").getAsString();
                log.info("成功获取AI回复，长度: {}", content.length());
                return content;
            }
            
            log.warn("DeepSeek响应格式异常，无choices数组");
            return "抱歉，我暂时无法回答这个问题。";
            
        } catch (java.net.SocketTimeoutException e) {
            log.error("DeepSeek API调用超时", e);
            return "抱歉，网络连接超时，请检查网络后重试。";
        } catch (java.io.IOException e) {
            log.error("DeepSeek API网络错误", e);
            return "抱歉，网络连接失败，请检查网络设置。";
        } catch (Exception e) {
            log.error("DeepSeek Chat调用失败", e);
            return "抱歉，服务暂时不可用：" + e.getMessage();
        }
    }

    /**
     * 获取默认推荐（当API调用失败时）
     */
    private List<Map<String, String>> getDefaultRecommendations(int count) {
        List<Map<String, String>> defaults = new ArrayList<>();
        
        Map<String, String> job1 = new HashMap<>();
        job1.put("name", "产品经理");
        job1.put("reason", "综合技能要求高，适合多元化背景人才");
        defaults.add(job1);

        Map<String, String> job2 = new HashMap<>();
        job2.put("name", "项目经理");
        job2.put("reason", "注重沟通协调，适合组织能力强的人");
        defaults.add(job2);

        Map<String, String> job3 = new HashMap<>();
        job3.put("name", "业务分析师");
        job3.put("reason", "连接技术与业务，发展前景广阔");
        defaults.add(job3);

        return defaults.subList(0, Math.min(count, defaults.size()));
    }
}
