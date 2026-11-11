package ua.sumdu.staff.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ua.sumdu.staff.DatabaseTestBase;
import ua.sumdu.staff.exception.DataAccessException;

class DatabaseTest extends DatabaseTestBase {

    @Test
    void connectsWithValidSettings() {
        assertDoesNotThrow(() -> db.checkConnection());
    }

    @Test
    void reportsMissingDatabase() {
        Database wrong = new Database(new ConnectionSettings("localhost", port(), "no_such_db", "postgres", ""));
        DataAccessException ex = assertThrows(DataAccessException.class, wrong::checkConnection);
        assertTrue(ex.getMessage().contains("не знайдено"), ex.getMessage());
    }

    @Test
    void reportsUnreachableServer() {
        Database wrong = new Database(new ConnectionSettings("localhost", 1, "postgres", "postgres", ""));
        DataAccessException ex = assertThrows(DataAccessException.class, wrong::checkConnection);
        assertTrue(ex.getMessage().contains("з'єднатися"), ex.getMessage());
    }

    @Test
    void rollsBackTransactionOnError() {
        assertThrows(DataAccessException.class, () -> db.inTransaction(c -> {
            try (var st = c.createStatement()) {
                st.executeUpdate("DELETE FROM employees");
                st.executeUpdate("INSERT INTO departments (name) VALUES (NULL)");
            }
            return null;
        }));
        int count = db.withConnection(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM employees")) {
                rs.next();
                return rs.getInt(1);
            }
        });
        assertTrue(count == 18, "видалення відкочено");
    }
}
