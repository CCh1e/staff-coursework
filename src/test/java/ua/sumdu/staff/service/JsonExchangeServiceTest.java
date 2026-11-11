package ua.sumdu.staff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.sumdu.staff.DatabaseTestBase;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.ImportResult;

class JsonExchangeServiceTest extends DatabaseTestBase {

    @TempDir
    Path tmp;

    private void clearTables() throws SQLException {
        try (Connection c = db.openConnection(); Statement st = c.createStatement()) {
            st.execute("TRUNCATE employees, departments RESTART IDENTITY");
        }
    }

    @Test
    void exportThenImportIntoEmptyDatabaseRestoresData() throws Exception {
        Path file = tmp.resolve("export.json");
        assertEquals(18, context.jsonExchange().exportTo(file));
        String json = Files.readString(file);
        assertTrue(json.contains("\"hireDate\" : \"2012-03-15\""), "дати пишуться у форматі ISO");
        assertTrue(json.contains("ІТ-відділ"));

        clearTables();
        ImportResult result = context.jsonExchange().importFrom(file);

        assertEquals(new ImportResult(6, 0, 18, 0), result);
        assertEquals(18, context.employees().getAll().size());
        assertEquals(new java.math.BigDecimal("651500.00"), context.departments().getSalaryFundReport().total());
    }

    @Test
    void importIntoSameDatabaseUpdatesInsteadOfDuplicating() {
        Path file = tmp.resolve("export.json");
        context.jsonExchange().exportTo(file);

        ImportResult result = context.jsonExchange().importFrom(file);

        assertEquals(new ImportResult(0, 6, 0, 18), result);
        assertEquals(18, context.employees().getAll().size());
    }

    @Test
    void invalidRecordCancelsWholeImport() throws IOException {
        Path file = tmp.resolve("bad.json");
        Files.writeString(file, """
                {
                  "version": 1,
                  "departments": [ { "name": "Новий відділ" } ],
                  "employees": [
                    { "lastName": "Добрий", "firstName": "Запис", "position": "Тест",
                      "salary": 1000, "hireDate": "2020-01-01", "department": "Новий відділ" },
                    { "lastName": "", "firstName": "Без прізвища", "position": "Тест",
                      "salary": -1, "hireDate": "2020-01-01", "department": "Новий відділ" }
                  ]
                }
                """);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> context.jsonExchange().importFrom(file));

        assertTrue(ex.getMessage().contains("Працівник №2"), ex.getMessage());
        assertEquals(6, context.departments().getAll().size(), "транзакцію відкочено");
        assertEquals(18, context.employees().getAll().size());
    }

    @Test
    void reportsUnknownDepartmentAndMalformedJson() throws IOException {
        Path unknown = tmp.resolve("unknown.json");
        Files.writeString(unknown, """
                { "employees": [ { "lastName": "Тест", "firstName": "Тест", "position": "Тест",
                  "salary": 1, "hireDate": "2020-01-01", "department": "Неіснуючий" } ] }
                """);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> context.jsonExchange().importFrom(unknown));
        assertTrue(ex.getMessage().contains("Неіснуючий"));

        Path broken = tmp.resolve("broken.json");
        Files.writeString(broken, "{ not json");
        assertThrows(ValidationException.class, () -> context.jsonExchange().importFrom(broken));

        assertThrows(ValidationException.class, () -> context.jsonExchange().importFrom(tmp.resolve("missing.json")));
    }
}
