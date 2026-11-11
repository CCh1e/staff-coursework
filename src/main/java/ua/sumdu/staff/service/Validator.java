package ua.sumdu.staff.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.Employee;

/**
 * Перевірка коректності даних перед записом до бази. Також прибирає зайві пробіли.
 */
public final class Validator {

    public static final BigDecimal MAX_SALARY = new BigDecimal("10000000");
    public static final LocalDate MIN_HIRE_DATE = LocalDate.of(1950, 1, 1);

    private static final Pattern PERSON_NAME = Pattern.compile("^\\p{L}[\\p{L}'’ʼ\\- ]*$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9()\\- ]{5,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$");

    private Validator() {
    }

    public static void validate(Department d) {
        List<String> errors = new ArrayList<>();
        d.setName(trimToNull(d.getName()));
        d.setLocation(trimToNull(d.getLocation()));
        d.setPhone(trimToNull(d.getPhone()));

        requireText(errors, d.getName(), "Назва підрозділу", 100);
        optionalMaxLength(errors, d.getLocation(), "Розташування", 100);
        if (d.getPhone() != null && !PHONE.matcher(d.getPhone()).matches()) {
            errors.add("Телефон має містити від 5 до 20 символів: цифри, пробіли, дужки, дефіс, «+» на початку.");
        }
        throwIfAny(errors);
    }

    public static void validate(Employee e) {
        List<String> errors = new ArrayList<>();
        e.setLastName(trimToNull(e.getLastName()));
        e.setFirstName(trimToNull(e.getFirstName()));
        e.setMiddleName(trimToNull(e.getMiddleName()));
        e.setPosition(trimToNull(e.getPosition()));
        e.setEmail(trimToNull(e.getEmail()));

        requirePersonName(errors, e.getLastName(), "Прізвище", true);
        requirePersonName(errors, e.getFirstName(), "Ім'я", true);
        requirePersonName(errors, e.getMiddleName(), "По батькові", false);
        requireText(errors, e.getPosition(), "Посада", 100);

        BigDecimal salary = e.getSalary();
        if (salary == null) {
            errors.add("Поле «Зарплата» обов'язкове.");
        } else if (salary.signum() < 0) {
            errors.add("Зарплата не може бути від'ємною.");
        } else if (salary.compareTo(MAX_SALARY) > 0) {
            errors.add("Зарплата не може перевищувати " + MAX_SALARY.toPlainString() + ".");
        } else if (salary.stripTrailingZeros().scale() > 2) {
            errors.add("Зарплата може мати не більше двох знаків після коми.");
        }

        LocalDate hireDate = e.getHireDate();
        if (hireDate == null) {
            errors.add("Поле «Дата прийняття на роботу» обов'язкове.");
        } else if (hireDate.isAfter(LocalDate.now())) {
            errors.add("Дата прийняття на роботу не може бути в майбутньому.");
        } else if (hireDate.isBefore(MIN_HIRE_DATE)) {
            errors.add("Дата прийняття на роботу не може бути раніше " + MIN_HIRE_DATE + ".");
        }

        if (e.getEmail() != null) {
            if (e.getEmail().length() > 100 || !EMAIL.matcher(e.getEmail()).matches()) {
                errors.add("Email має некоректний формат.");
            }
        }
        if (e.getDepartmentId() <= 0) {
            errors.add("Оберіть підрозділ.");
        }
        throwIfAny(errors);
    }

    /**
     * Перетворює текст на суму. Дозволяє кому як десятковий роздільник і пробіли між розрядами.
     *
     * @throws ValidationException якщо текст не є числом
     */
    public static BigDecimal parseSalary(String text) {
        String normalized = text == null ? "" : text.replaceAll("[\\s\\u00A0]", "").replace(',', '.');
        if (normalized.isEmpty()) {
            throw new ValidationException("Поле «Зарплата» обов'язкове.");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Зарплата має бути числом, наприклад 25000 або 25000,50.");
        }
    }

    private static void requireText(List<String> errors, String value, String field, int maxLength) {
        if (value == null) {
            errors.add("Поле «" + field + "» обов'язкове.");
        } else {
            optionalMaxLength(errors, value, field, maxLength);
        }
    }

    private static void optionalMaxLength(List<String> errors, String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            errors.add("Поле «" + field + "» не може бути довшим за " + maxLength + " символів.");
        }
    }

    private static void requirePersonName(List<String> errors, String value, String field, boolean required) {
        if (value == null) {
            if (required) {
                errors.add("Поле «" + field + "» обов'язкове.");
            }
            return;
        }
        if (value.length() > 50) {
            errors.add("Поле «" + field + "» не може бути довшим за 50 символів.");
        } else if (!PERSON_NAME.matcher(value).matches()) {
            errors.add("Поле «" + field + "» може містити лише літери, апостроф, дефіс і пробіл.");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void throwIfAny(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
