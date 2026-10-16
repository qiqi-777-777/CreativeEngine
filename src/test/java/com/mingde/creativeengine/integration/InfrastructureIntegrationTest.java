package com.mingde.creativeengine.integration;

import com.mingde.creativeengine.service.PolicyDataService;
import com.mingde.creativeengine.storage.RedisTokenStore;
import com.mingde.creativeengine.storage.TokenStore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础设施集成测试：用真实容器跑之前「只有单测覆盖」或「完全没验过」的链路。
 *
 * <p>其中最有价值的是第一条——本地开发库是被 Flyway baselined 的，V1 建表脚本
 * 从来没有真正执行过，就算语法写错也不会有人发现。空库从零跑一遍才说明迁移链是通的。</p>
 *
 * <p>{@code disabledWithoutDocker = true}：本机没开 Docker 时整类跳过，
 * 而不是把 {@code mvn test} 直接跑挂。</p>
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("creative_engine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("app.redis.enabled", () -> true);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PolicyDataService policyDataService;

    @Test
    void flywayBuildsWholeSchemaOnEmptyDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);

        assertTrue(tables.containsAll(List.of("users", "policy_data", "occupation", "banner",
                        "agent_session_log", "user_bp_draft", "user_career_record",
                        "user_notification", "user_policy_favorite", "flyway_schema_history")),
                "空库执行 V1~V3 后应建出全部业务表，实际为：" + tables);
    }

    @Test
    void roleColumnIsAddedByV3Migration() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role'",
                Integer.class);

        assertEquals(1, count, "V3 迁移应在 users 表上补出 role 字段");
    }

    @Test
    void tokenStoreUsesRedisAndKeepsExpiry() {
        assertInstanceOf(RedisTokenStore.class, tokenStore, "开启 Redis 后应走 Redis 实现");

        tokenStore.save("integration-token", 123L);

        assertEquals(123L, tokenStore.resolve("integration-token"));
        Long ttl = stringRedisTemplate.getExpire("auth:token:integration-token");
        assertNotNull(ttl);
        assertTrue(ttl > 0, "真实 Redis 上必须带上过期时间，TTL=" + ttl);

        tokenStore.revoke("integration-token");
        assertTrue(stringRedisTemplate.getExpire("auth:token:integration-token") == -2L, "revoke 后 key 应被删除");
    }

    @Test
    void hotDataCacheFallsBackToRedis() {
        // 空库返回空列表，但空列表是非 null，依然应该写进缓存
        policyDataService.getAllPolicies();

        Set<String> keys = stringRedisTemplate.keys("policyList*");
        assertNotNull(keys);
        assertTrue(!keys.isEmpty(), "L1 未命中时结果应落到 Redis（L2），实际 keys=" + keys);
    }

    @Test
    void redisContainerIsReachable() {
        Duration ping = Duration.ofSeconds(5);
        assertNotNull(stringRedisTemplate.execute(connection -> connection.ping(), true),
                "Redis 容器应可连通，超时设置 " + ping);
    }
}
