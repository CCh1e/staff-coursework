package ua.sumdu.staff.exception;

import java.util.List;

/**
 * Некоректні дані, введені користувачем або прочитані з файлу.
 */
public class ValidationException extends AppException {

    private final List<String> errors;

    public ValidationException(String message) {
        this(List.of(message));
    }

    public ValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
