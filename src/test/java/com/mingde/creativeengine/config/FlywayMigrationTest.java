package com.mingde.creativeengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 迁移脚本测试：确认基线脚本覆盖全部业务表，且不夹带删表与写数据的语句。
 *
 * <p>只做文件校验，不连数据库；真正执行迁移由应用启动时的 Flyway 负责。</p>
 */
class FlywayMigrationTest {

    private static final String BASELINE = "db/migration/V1__init_schema.sql";

    private static final String AGENT_LOG_MIGRATION = "db/migration/V2__add_agent_session_log.sql";

    private static final List<String> TABLES = List.of(
            "banner", "occupation", "policy_data",
            "user_bp_draft", "user_career_record", "user_notification",
            "user_policy_favorite", "users");

    /** AgentSessionLog 实体用到的列，迁移脚本里必须都有。 */
    private static final List<String> AGENT_LOG_COLUMNS = List.of(
            "user_id", "agent_type", "input_text", "output_text", "related_id", "create_time");

    @Test
    void baselineMigrationCreatesEveryTable() throws Exception {
        String sql = read(BASELINE);

        for (String table : TABLES) {
            assertTrue(sql.contains("CREATE TABLE `" + table + "`"),
                    "V1 应包含 " + table + " 的建表语句");
        }
    }

    @Test
    void agentLogMigrationCreatesTableWithAllEntityColumns() throws Exception {
        String sql = read(AGENT_LOG_MIGRATION);

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `agent_session_log`"),
                "V2 应创建 agent_session_log 表");
        for (String column : AGENT_LOG_COLUMNS) {
            assertTrue(sql.contains("`" + column + "`"), "V2 应包含列 " + column);
        }
    }

    @Test
    void baselineMigrationIsNotDestructive() throws Exception {
        String statements = read(BASELINE).lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"))
                .toUpperCase(Locale.ROOT);

        assertFalse(statements.contains("DROP TABLE"), "迁移脚本不应包含删表语句");
        assertFalse(statements.contains("INSERT INTO"), "迁移脚本不应夹带数据写入");
    }

    private String read(String path) throws Exception {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
