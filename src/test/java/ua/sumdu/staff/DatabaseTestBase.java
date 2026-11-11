package ua.sumdu.staff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ua.sumdu.staff.db.ConnectionSettings;
import ua.sumdu.staff.db.Database;

/**
 * Запускає справжній PostgreSQL для тестів і перед кожним тестом відновлює
 * структуру та тестові дані зі скриптів sql/schema.sql і sql/data.sql.
 */
public abstract class DatabaseTestBase {

    private static EmbeddedPostgres postgres;

    protected Database db;
    protected AppContext context;

    @BeforeAll
    static void startPostgres() throws IOException {
        postgres = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        if (postgres != null) {
            postgres.close();
        }
    }

    @BeforeEach
    void resetDatabase() throws IOException, SQLException {
        db = new Database(new ConnectionSettings("localhost", postgres.getPort(), "postgres", "postgres", ""));
        try (Connection c = db.openConnection(); Statement st = c.createStatement()) {
            st.execute(Files.readString(Path.of("sql", "schema.sql")));
            st.execute(Files.readString(Path.of("sql", "data.sql")));
        }
        context = new AppContext(db);
    }

    protected static int port() {
        return postgres.getPort();
    }
}
