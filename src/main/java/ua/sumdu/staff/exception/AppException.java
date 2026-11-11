package ua.sumdu.staff.exception;

/**
 * Базовий виняток застосунку. Повідомлення призначене для показу користувачу.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
