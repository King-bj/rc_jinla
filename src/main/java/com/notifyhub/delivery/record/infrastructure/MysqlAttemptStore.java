package com.notifyhub.delivery.record.infrastructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

/** 追加投递尝试。不决定任务是否重试。 */
public final class MysqlAttemptStore {

    private final Connection connection;

    public MysqlAttemptStore(Connection connection) {
        this.connection = connection;
    }

    public static void createTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS delivery_attempt (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      idempotency_key VARCHAR(256) NOT NULL,
                      outcome VARCHAR(32) NOT NULL,
                      attempted_at TIMESTAMP NOT NULL
                    )""");
        }
    }

    public void insert(String idempotencyKey, String outcome, Instant attemptedAt) {
        String sql = "INSERT INTO delivery_attempt (idempotency_key, outcome, attempted_at) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            statement.setString(2, outcome);
            statement.setTimestamp(3, Timestamp.from(attemptedAt));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
