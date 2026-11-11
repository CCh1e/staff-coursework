package ua.sumdu.staff.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.Employee;

class ValidatorTest {

    private static Employee validEmployee() {
        return new Employee(0, "Мельник", "Ольга", "Іванівна", "Інженер",
                new BigDecimal("25000.50"), LocalDate.of(2020, 1, 15), "o.melnyk@test.ua", 1);
    }

    @Test
    void acceptsValidEmployeeAndTrimsFields() {
        Employee e = validEmployee();
        e.setLastName("  Мельник ");
        e.setMiddleName("   ");
        assertDoesNotThrow(() -> Validator.validate(e));
        assertEquals("Мельник", e.getLastName());
        assertNull(e.getMiddleName());
    }

    @Test
    void acceptsApostropheAndHyphenInNames() {
        Employee e = validEmployee();
        e.setLastName("Сем'янів-Коваль");
        e.setFirstName("Мар’яна");
        assertDoesNotThrow(() -> Validator.validate(e));
    }

    @Test
    void collectsAllErrorsAtOnce() {
        Employee e = new Employee(0, "", "123", null, null, new BigDecimal("-1"),
                LocalDate.now().plusDays(1), "not-an-email", 0);
        ValidationException ex = assertThrows(ValidationException.class, () -> Validator.validate(e));
        // прізвище, ім'я, посада, зарплата, дата, email, підрозділ
        assertEquals(7, ex.getErrors().size(), ex.getMessage());
    }

    @Test
    void rejectsSalaryWithMoreThanTwoDecimals() {
        Employee e = validEmployee();
        e.setSalary(new BigDecimal("100.123"));
        assertThrows(ValidationException.class, () -> Validator.validate(e));
    }

    @Test
    void parsesSalaryWithCommaAndSpaces() {
        assertEquals(new BigDecimal("25000.50"), Validator.parseSalary(" 25 000,50 "));
        assertThrows(ValidationException.class, () -> Validator.parseSalary("abc"));
        assertThrows(ValidationException.class, () -> Validator.parseSalary(""));
    }

    @Test
    void validatesDepartment() {
        assertDoesNotThrow(() -> Validator.validate(new Department(0, "Відділ якості", null, "+380 (542) 12-34-56")));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> Validator.validate(new Department(0, " ", null, "abc")));
        assertEquals(2, ex.getErrors().size());
        assertTrue(ex.getMessage().contains("Назва підрозділу"));
    }
}
