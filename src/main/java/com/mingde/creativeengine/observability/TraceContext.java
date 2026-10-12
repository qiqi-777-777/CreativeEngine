package com.mingde.creativeengine.observability;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 请求级 traceId。
 *
 * <p>一次 Agent 问答会串起"模型调用可能多轮 + 工具调用若干次"的链路，
 * 只靠日志时间戳很难把它们对上，因此给每个请求分配一个 traceId 落到 MDC，
 * 模型与工具的监听器都把它一起打出来。</p>
 */
public final class TraceContext {

    /** 写入 MDC 与响应体的键名，日志模板通过 %X{traceId} 引用。 */
    public static final String TRACE_ID_KEY = "traceId";

    /** 允许调用方（网关/前端）透传自己的 traceId。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceContext() {
    }

    /**
     * 生成一个短 traceId（16 位十六进制，既便于日志检索也不至于太长）。
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 规整外部传入的 traceId：为空或格式明显异常时重新生成，避免把脏数据写进日志。
     */
    public static String normalize(String incoming) {
        if (incoming == null) {
            return generate();
        }
        String trimmed = incoming.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            return generate();
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean allowed = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-' || c == '_';
            if (!allowed) {
                return generate();
            }
        }
        return trimmed;
    }

    public static void begin(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 当前请求的 traceId，不在请求线程里（如定时任务）时返回占位符。
     */
    public static String current() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId == null || traceId.isEmpty() ? "-" : traceId;
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
