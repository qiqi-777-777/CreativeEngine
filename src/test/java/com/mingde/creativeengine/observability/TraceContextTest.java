package com.mingde.creativeengine.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * traceId 测试：验证生成、规整与 MDC 生命周期，确保请求结束后不残留。
 */
class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void generateProducesShortHexId() {
        String traceId = TraceContext.generate();
        assertEquals(16, traceId.length());
        assertTrue(traceId.matches("[0-9a-f]{16}"), traceId);
    }

    @Test
    void normalizeKeepsWellFormedIncomingId() {
        assertEquals("abc-123_XY", TraceContext.normalize("abc-123_XY"));
        assertEquals("spaced", TraceContext.normalize("  spaced  "));
    }

    @Test
    void normalizeRegeneratesBlankOrDirtyId() {
        assertTrue(TraceContext.normalize(null).matches("[0-9a-f]{16}"));
        assertTrue(TraceContext.normalize("").matches("[0-9a-f]{16}"));
        assertTrue(TraceContext.normalize("   ").matches("[0-9a-f]{16}"));
        assertTrue(TraceContext.normalize("bad id with space").matches("[0-9a-f]{16}"));
        assertTrue(TraceContext.normalize("a".repeat(65)).matches("[0-9a-f]{16}"));
    }

    @Test
    void beginCurrentAndClearManageMdc() {
        assertEquals("-", TraceContext.current(), "未开始时返回占位符");

        TraceContext.begin("trace-abc");
        assertEquals("trace-abc", TraceContext.current());

        TraceContext.clear();
        assertEquals("-", TraceContext.current(), "清理后不应残留 traceId");
    }

    @Test
    void currentIsStableWithinSameThread() {
        TraceContext.begin("trace-1");
        assertEquals(TraceContext.current(), TraceContext.current());
        assertNotEquals("-", TraceContext.current());
        assertFalse(TraceContext.current().isEmpty());
    }
}
