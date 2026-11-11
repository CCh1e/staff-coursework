package ua.sumdu.staff.model;

/**
 * Підсумок імпорту даних із JSON-файлу.
 */
public record ImportResult(int departmentsAdded, int departmentsUpdated,
                           int employeesAdded, int employeesUpdated) {

    @Override
    public String toString() {
        return "Підрозділи: додано " + departmentsAdded + ", оновлено " + departmentsUpdated + "\n"
                + "Працівники: додано " + employeesAdded + ", оновлено " + employeesUpdated;
    }
}
