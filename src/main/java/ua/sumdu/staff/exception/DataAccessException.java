package ua.sumdu.staff.exception;

/**
 * Помилка під час роботи з базою даних або файлами.
 */
public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
