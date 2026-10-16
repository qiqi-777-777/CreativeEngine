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

    private static final List<String> TABLES = List.of(
            "banner", "occupation", "policy_data",
            "user_bp_draft", "user_career_record", "user_notification",
            "user_policy_favorite", "users");

    @Test
    void baselineMigrationCreatesEveryTable() throws Exception {
        String sql = read(BASELINE);

        for (String table : TABLES) {
            assertTrue(sql.contains("CREATE TABLE `" + table + "`"),
                    "V1 应包含 " + table + " 的建表语句");
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
