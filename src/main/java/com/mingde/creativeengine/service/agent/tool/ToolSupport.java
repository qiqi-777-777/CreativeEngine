package com.mingde.creativeengine.service.agent.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 工具层的文本处理助手。
 *
 * <p>工具返回值直接进入模型上下文，因此需要统一的空值兜底与长度截断，
 * 避免某次工具调用把上下文顶满。</p>
 */
final class ToolSupport {

    private ToolSupport() {
    }

    static String defaultValue(String value) {
        return value == null || value.trim().isEmpty() ? "未提供" : value.trim();
    }

    static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "……（内容过长，已截断）";
    }

    /**
     * 把模型传来的标签串拆成列表。模型可能用顿号、逗号、分号或空格分隔，
     * 也可能直接传 JSON 数组样式的字符串，这里统一处理。
     */
    static List<String> splitTags(String raw) {
        List<String> tags = new ArrayList<>();
        if (raw == null) {
            return tags;
        }
        String cleaned = raw.replace("[", "").replace("]", "").replace("\"", "");
        for (String part : Arrays.asList(cleaned.split("[,，、;；\\s]+"))) {
            String tag = part.trim();
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    static String joinLimit(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        if (values.size() <= limit) {
            return String.join("、", values);
        }
        return String.join("、", values.subList(0, limit)) + " 等 " + values.size() + " 项";
    }
}
