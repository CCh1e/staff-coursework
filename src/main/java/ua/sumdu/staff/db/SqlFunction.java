package ua.sumdu.staff.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Дія з відкритим з'єднанням, яка може кинути {@link SQLException}.
 */
@FunctionalInterface
public interface SqlFunction<T> {
    T apply(Connection connection) throws SQLException;
}
