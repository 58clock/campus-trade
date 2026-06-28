package com.campus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `browse_history` (
                    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
                    `user_id`     BIGINT       NOT NULL,
                    `product_id`  BIGINT       NOT NULL,
                    `category`    VARCHAR(50)  NOT NULL,
                    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    KEY `idx_user_id` (`user_id`),
                    KEY `idx_category` (`category`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            log.info(">>> browse_history table ready <<<");
        } catch (Exception e) {
            log.error("!!! Failed to create browse_history table: {}", e.getMessage());
        }
    }
}
