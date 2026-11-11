package ua.sumdu.staff.db;

import java.sql.SQLException;

import ua.sumdu.staff.exception.DataAccessException;

/**
 * Перетворює {@link SQLException} на зрозуміле користувачу повідомлення за кодом SQLSTATE.
 */
public final class SqlErrors {

    private SqlErrors() {
    }

    public static DataAccessException translate(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String message = switch (state) {
            case "23505" -> "Запис з такими даними вже існує (порушено унікальність: назва підрозділу або email).";
            case "23503" -> "Операцію не виконано: запис пов'язаний з іншими даними "
                    + "(наприклад, у підрозділі є працівники).";
            case "23502" -> "Не заповнено обов'язкове поле.";
            case "23514" -> "Значення не відповідає обмеженням бази даних (наприклад, від'ємна зарплата).";
            case "28P01", "28000" -> "Невірний логін або пароль користувача бази даних.";
            case "3D000" -> "Базу даних з такою назвою не знайдено.";
            case "42P01" -> "У базі даних немає потрібних таблиць. Виконайте скрипт sql/schema.sql.";
            default -> state.startsWith("08")
                    ? "Не вдалося з'єднатися із сервером PostgreSQL. Перевірте адресу, порт і чи запущено сервер."
                    : "Помилка бази даних: " + e.getMessage();
        };
        return new DataAccessException(message, e);
    }
}
