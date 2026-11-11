package ua.sumdu.staff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import ua.sumdu.staff.DatabaseTestBase;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Employee;

class EmployeeServiceTest extends DatabaseTestBase {

    private Employee newEmployee(String email) {
        int departmentId = context.departments().getAll().getFirst().getId();
        return new Employee(0, "Іваненко", "Петро", null, "Економіст",
                new BigDecimal("30000"), LocalDate.of(2024, 5, 1), email, departmentId);
    }

    @Test
    void addsEmployee() {
        Employee e = context.employees().add(newEmployee("p.ivanenko@company.ua"));
        assertTrue(e.getId() > 0);
        assertEquals(19, context.employees().getAll().size());
    }

    @Test
    void rejectsDuplicateEmail() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> context.employees().add(newEmployee("O.Kovalenko@company.ua")));
        assertTrue(ex.getMessage().contains("Коваленко"));
        assertEquals(18, context.employees().getAll().size());
    }

    @Test
    void rejectsInvalidEmployee() {
        Employee e = newEmployee(null);
        e.setSalary(new BigDecimal("-5"));
        assertThrows(ValidationException.class, () -> context.employees().add(e));
    }

    @Test
    void rejectsUnknownDepartment() {
        Employee e = newEmployee(null);
        e.setDepartmentId(999_999);
        assertThrows(ValidationException.class, () -> context.employees().add(e));
    }

    @Test
    void updatesAndDeletesEmployee() {
        Employee e = context.employees().getAll().getFirst();
        e.setPosition("Старший спеціаліст");
        e.setSalary(new BigDecimal("31000.00"));
        context.employees().update(e);

        Employee reloaded = context.employees().getAll().stream()
                .filter(x -> x.getId() == e.getId()).findFirst().orElseThrow();
        assertEquals("Старший спеціаліст", reloaded.getPosition());
        assertEquals(new BigDecimal("31000.00"), reloaded.getSalary());

        context.employees().delete(e.getId());
        assertEquals(17, context.employees().getAll().size());
        assertThrows(ValidationException.class, () -> context.employees().delete(e.getId()));
    }
}
