package ua.sumdu.staff.db;

/**
 * Параметри підключення до PostgreSQL, які вводить користувач у вікні входу.
 */
public record ConnectionSettings(String host, int port, String database, String user, String password) {

    public String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    /** Пароль не виводиться, щоб не потрапив у журнали. */
    @Override
    public String toString() {
        return user + "@" + host + ":" + port + "/" + database;
    }
}
