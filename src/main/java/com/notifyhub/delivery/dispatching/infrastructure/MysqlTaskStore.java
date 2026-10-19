package com.notifyhub.delivery.dispatching.infrastructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

/** 保存消息和任务。不判断状态机，只负责写入。 */
public final class MysqlTaskStore {

    private final Connection connection;

    public MysqlTaskStore(Connection connection) {
        this.connection = connection;
    }

    public static void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS delivery_message (
                      tenant_id VARCHAR(64) NOT NULL,
                      event_id VARCHAR(64) NOT NULL,
                      PRIMARY KEY (tenant_id, event_id)
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS delivery_task (
                      tenant_id VARCHAR(64) NOT NULL,
                      event_id VARCHAR(64) NOT NULL,
                      destination_id VARCHAR(64) NOT NULL,
                      status VARCHAR(32) NOT NULL,
                      config_version BIGINT NOT NULL,
                      idempotency_key VARCHAR(256) NOT NULL,
                      PRIMARY KEY (tenant_id, event_id, destination_id)
                    )""");
        }
    }

    public void insertMessage(String tenantId, String eventId) {
        update("INSERT INTO delivery_message (tenant_id, event_id) VALUES (?, ?)", tenantId, eventId);
    }

    public void insertTask(String tenantId, String eventId, String destinationId, String status,
                            long configVersion, String idempotencyKey) {
        update("""
                INSERT INTO delivery_task
                  (tenant_id, event_id, destination_id, status, config_version, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?)""",
                tenantId, eventId, destinationId, status, configVersion, idempotencyKey);
    }

    private void update(String sql, Object... args) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new DuplicateTaskException(ex);
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                throw new DuplicateTaskException(ex);
            }
            throw new IllegalStateException(ex);
        }
    }
}
