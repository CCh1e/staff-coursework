package ua.sumdu.staff.model;

import java.math.BigDecimal;

/**
 * Сумарний фонд заробітної плати одного підрозділу, отриманий із бази даних.
 */
public record DepartmentFund(int departmentId, String departmentName, int employeeCount, BigDecimal fund) {
}
