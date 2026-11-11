package ua.sumdu.staff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Запускає тимчасовий PostgreSQL на порту 5433 з базою staff_db і тестовими даними,
 * щоб перевірити застосунок без встановлення PostgreSQL. Користувач postgres, пароль порожній.
 * Зупинка — Ctrl+C.
 */
public final class DemoDatabase {

    public static final int PORT = 5433;

    private DemoDatabase() {
    }

    public static void main(String[] args) throws Exception {
        EmbeddedPostgres pg = EmbeddedPostgres.builder().setPort(PORT).start();
        try (Connection c = pg.getPostgresDatabase().getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE staff_db");
        }
        String url = "jdbc:postgresql://localhost:" + PORT + "/staff_db";
        try (Connection c = DriverManager.getConnection(url, "postgres", ""); Statement st = c.createStatement()) {
            st.execute(Files.readString(Path.of("sql", "schema.sql")));
            st.execute(Files.readString(Path.of("sql", "data.sql")));
        }
        System.out.println("DEMO DATABASE READY: localhost:" + PORT + "/staff_db, user postgres, empty password");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pg.close();
            } catch (Exception ignored) {
                // процес однаково завершується
            }
        }));
        Thread.currentThread().join();
    }
}
