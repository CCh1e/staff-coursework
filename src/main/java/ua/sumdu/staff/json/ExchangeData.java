package ua.sumdu.staff.json;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Структура JSON-файлу для експорту та імпорту. Працівник посилається на підрозділ
 * за назвою, а не за id, бо в іншій базі ідентифікатори можуть відрізнятися.
 */
public record ExchangeData(int version, LocalDateTime exportedAt,
                           List<DepartmentDto> departments, List<EmployeeDto> employees) {

    public static final int CURRENT_VERSION = 1;

    public record DepartmentDto(String name, String location, String phone) {
    }

    public record EmployeeDto(String lastName, String firstName, String middleName, String position,
                              BigDecimal salary, LocalDate hireDate, String email, String department) {
    }
}
