package com.notifyhub.delivery.dispatching.infrastructure;

import com.notifyhub.delivery.record.infrastructure.MysqlAttemptStore;
import com.notifyhub.delivery.routing.infrastructure.MysqlRouteStore;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MysqlTaskStoreTest {

    @Test
    void duplicateTaskIsRejected() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:delivery;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            MysqlTaskStore.createTables(connection);
            MysqlAttemptStore.createTable(connection);
            MysqlRouteStore.createTable(connection);

            MysqlTaskStore tasks = new MysqlTaskStore(connection);
            tasks.insertMessage("tenant", "event");
            tasks.insertTask("tenant", "event", "crm", "QUEUED", 4, "tenant:event:crm");
            new MysqlAttemptStore(connection).insert("tenant:event:crm", "SUCCESS", Instant.parse("2026-10-19T09:00:00Z"));
            new MysqlRouteStore(connection).insert("tenant", "crm", 4, true);

            assertDoesNotThrow(() -> tasks.insertTask("tenant", "event", "inventory", "QUEUED", 4, "tenant:event:inventory"));
            assertThrows(DuplicateTaskException.class, () ->
                    tasks.insertTask("tenant", "event", "crm", "QUEUED", 9, "tenant:event:crm"));
        }
    }
}
