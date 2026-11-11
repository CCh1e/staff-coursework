package ua.sumdu.staff.model;

/**
 * Рядок списку підрозділів разом із кількістю працівників у кожному з них.
 */
public record DepartmentStats(Department department, int employeeCount) {

    public int id() {
        return department.getId();
    }

    public String name() {
        return department.getName();
    }
}
