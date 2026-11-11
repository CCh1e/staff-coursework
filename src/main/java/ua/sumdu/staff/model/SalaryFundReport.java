package ua.sumdu.staff.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Звіт про загальний фонд заробітної плати та частку кожного підрозділу в ньому.
 */
public record SalaryFundReport(List<Entry> entries, BigDecimal total) {

    public SalaryFundReport {
        entries = List.copyOf(entries);
    }

    /**
     * @param sharePercent частка підрозділу у загальному фонді, % (два знаки після коми)
     */
    public record Entry(int departmentId, String departmentName, int employeeCount,
                        BigDecimal fund, BigDecimal sharePercent) {
    }
}
