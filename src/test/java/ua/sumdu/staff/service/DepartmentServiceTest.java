package ua.sumdu.staff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import ua.sumdu.staff.DatabaseTestBase;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.DepartmentFund;
import ua.sumdu.staff.model.DepartmentStats;
import ua.sumdu.staff.model.SalaryFundReport;
import ua.sumdu.staff.model.SortOrder;

class DepartmentServiceTest extends DatabaseTestBase {

    private Map<String, Integer> counts() {
        return context.departments().getStatistics(SortOrder.DESC).stream()
                .collect(Collectors.toMap(DepartmentStats::name, DepartmentStats::employeeCount));
    }

    private int idOf(String name) {
        return context.departments().getAll().stream()
                .filter(d -> d.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    @Test
    void countsEmployeesInEachDepartment() {
        Map<String, Integer> counts = counts();
        assertEquals(6, counts.size());
        assertEquals(5, counts.get("ІТ-відділ"));
        assertEquals(4, counts.get("Відділ продажів"));
        assertEquals(1, counts.get("Юридичний відділ"));
    }

    @Test
    void includesDepartmentsWithoutEmployees() {
        context.departments().add(new Department(0, "Новий відділ", null, null));
        assertEquals(0, counts().get("Новий відділ"));
    }

    @Test
    void sortsByEmployeeCount() {
        List<Integer> desc = context.departments().getStatistics(SortOrder.DESC).stream()
                .map(DepartmentStats::employeeCount).toList();
        List<Integer> asc = context.departments().getStatistics(SortOrder.ASC).stream()
                .map(DepartmentStats::employeeCount).toList();
        assertEquals(List.of(5, 4, 3, 3, 2, 1), desc);
        assertEquals(List.of(1, 2, 3, 3, 4, 5), asc);
    }

    @Test
    void transfersAllEmployeesToAnotherDepartment() {
        int it = idOf("ІТ-відділ");
        int legal = idOf("Юридичний відділ");

        int moved = context.departments().transferAllEmployees(it, legal);

        assertEquals(5, moved);
        assertEquals(0, counts().get("ІТ-відділ"));
        assertEquals(6, counts().get("Юридичний відділ"));
        assertTrue(context.employees().getByDepartment(legal).stream()
                .allMatch(e -> e.getDepartmentId() == legal));
    }

    @Test
    void rejectsTransferToSameOrEmptyDepartment() {
        int it = idOf("ІТ-відділ");
        assertThrows(ValidationException.class, () -> context.departments().transferAllEmployees(it, it));

        Department empty = context.departments().add(new Department(0, "Порожній відділ", null, null));
        assertThrows(ValidationException.class,
                () -> context.departments().transferAllEmployees(empty.getId(), it));
        assertThrows(ValidationException.class,
                () -> context.departments().transferAllEmployees(it, 999_999));
        assertEquals(5, counts().get("ІТ-відділ"));
    }

    @Test
    void calculatesSalaryFundAndShares() {
        SalaryFundReport report = context.departments().getSalaryFundReport();

        assertEquals(new BigDecimal("651500.00"), report.total());
        SalaryFundReport.Entry it = report.entries().stream()
                .filter(e -> e.departmentName().equals("ІТ-відділ")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("238000.00"), it.fund());
        assertEquals(new BigDecimal("36.53"), it.sharePercent());
        assertEquals("ІТ-відділ", report.entries().getFirst().departmentName(), "найбільший фонд першим");

        BigDecimal sumOfShares = report.entries().stream()
                .map(SalaryFundReport.Entry::sharePercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(sumOfShares.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.05")) <= 0,
                "сума часток ≈ 100 %, отримано " + sumOfShares);
    }

    @Test
    void salaryFundReportHandlesZeroTotal() {
        SalaryFundReport report = DepartmentService.buildReport(List.of(
                new DepartmentFund(1, "А", 0, BigDecimal.ZERO),
                new DepartmentFund(2, "Б", 0, BigDecimal.ZERO)));
        assertEquals(0, report.total().signum());
        assertTrue(report.entries().stream().allMatch(e -> e.sharePercent().signum() == 0));
    }

    @Test
    void doesNotDeleteDepartmentWithEmployees() {
        int it = idOf("ІТ-відділ");
        ValidationException ex = assertThrows(ValidationException.class, () -> context.departments().delete(it));
        assertTrue(ex.getMessage().contains("5"));

        Department empty = context.departments().add(new Department(0, "Тимчасовий", null, null));
        context.departments().delete(empty.getId());
        assertEquals(6, context.departments().getAll().size());
    }

    @Test
    void rejectsDuplicateDepartmentName() {
        assertThrows(ValidationException.class,
                () -> context.departments().add(new Department(0, "іт-відділ", null, null)));
    }

    @Test
    void updatesDepartment() {
        Department d = context.departments().getAll().getFirst();
        d.setLocation("Новий корпус");
        context.departments().update(d);
        assertEquals("Новий корпус", context.departments().getAll().stream()
                .filter(x -> x.getId() == d.getId()).findFirst().orElseThrow().getLocation());
    }
}
