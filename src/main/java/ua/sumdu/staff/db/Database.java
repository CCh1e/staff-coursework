package ua.sumdu.staff.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Відкриває з'єднання з PostgreSQL через JDBC і керує транзакціями.
 * Сервіси передають сюди дію, а цей клас відкриває з'єднання, фіксує або відкочує
 * транзакцію та закриває з'єднання.
 */
public class Database {

    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private final ConnectionSettings settings;

    public Database(ConnectionSettings settings) {
        this.settings = settings;
    }

    public ConnectionSettings getSettings() {
        return settings;
    }

    public Connection openConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", settings.user());
        props.setProperty("password", settings.password());
        props.setProperty("loginTimeout", String.valueOf(LOGIN_TIMEOUT_SECONDS));
        props.setProperty("ApplicationName", "staff-coursework");
        return DriverManager.getConnection(settings.jdbcUrl(), props);
    }

    /** Перевіряє логін, пароль і наявність потрібних таблиць. */
    public void checkConnection() {
        withConnection(connection -> {
            try (var st = connection.createStatement()) {
                st.executeQuery("SELECT 1 FROM departments LIMIT 1").close();
                st.executeQuery("SELECT 1 FROM employees LIMIT 1").close();
            }
            return null;
        });
    }

    /** Виконує дію в режимі автофіксації (для одиночних запитів). */
    public <T> T withConnection(SqlFunction<T> work) {
        try (Connection connection = openConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw SqlErrors.translate(e);
        }
    }

    /**
     * Виконує дію в одній транзакції: усі зміни фіксуються разом або
     * повністю скасовуються при будь-якій помилці.
     */
    public <T> T inTransaction(SqlFunction<T> work) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw SqlErrors.translate(e);
        }
    }
}
