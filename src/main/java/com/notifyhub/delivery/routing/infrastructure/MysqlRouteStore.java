package com.notifyhub.delivery.routing.infrastructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** 保存路由配置。不执行路由匹配。 */
public final class MysqlRouteStore {

    private final Connection connection;

    public MysqlRouteStore(Connection connection) {
        this.connection = connection;
    }

    public static void createTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS delivery_route (
                      tenant_id VARCHAR(64) NOT NULL,
                      destination_id VARCHAR(64) NOT NULL,
                      config_version BIGINT NOT NULL,
                      enabled BOOLEAN NOT NULL,
                      PRIMARY KEY (tenant_id, destination_id, config_version)
                    )""");
        }
    }

    public void insert(String tenantId, String destinationId, long configVersion, boolean enabled) {
        String sql = """
                INSERT INTO delivery_route (tenant_id, destination_id, config_version, enabled)
                VALUES (?, ?, ?, ?)""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            statement.setString(2, destinationId);
            statement.setLong(3, configVersion);
            statement.setBoolean(4, enabled);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
