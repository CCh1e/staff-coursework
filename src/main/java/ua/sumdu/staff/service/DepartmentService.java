package ua.sumdu.staff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ua.sumdu.staff.dao.DepartmentDao;
import ua.sumdu.staff.dao.EmployeeDao;
import ua.sumdu.staff.db.Database;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.DepartmentFund;
import ua.sumdu.staff.model.DepartmentStats;
import ua.sumdu.staff.model.SalaryFundReport;
import ua.sumdu.staff.model.SortOrder;

/**
 * Операції з підрозділами, зокрема функції варіанта 4:
 * список із кількістю працівників, переведення працівників і фонд заробітної плати.
 */
public class DepartmentService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Database db;
    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;

    public DepartmentService(Database db, DepartmentDao departmentDao, EmployeeDao employeeDao) {
        this.db = db;
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
    }

    public List<Department> getAll() {
        return db.withConnection(departmentDao::findAll);
    }

    /** Підрозділи з кількістю працівників, відсортовані за цією кількістю. */
    public List<DepartmentStats> getStatistics(SortOrder order) {
        return db.withConnection(c -> departmentDao.findAllWithEmployeeCount(c, order));
    }

    public Department add(Department department) {
        Validator.validate(department);
        return db.inTransaction(c -> {
            ensureNameIsFree(c, department);
            departmentDao.insert(c, department);
            return department;
        });
    }

    public void update(Department department) {
        Validator.validate(department);
        db.inTransaction(c -> {
            ensureNameIsFree(c, department);
            if (!departmentDao.update(c, department)) {
                throw new ValidationException("Підрозділ не знайдено. Можливо, його вже видалено.");
            }
            return null;
        });
    }

    /** Видаляє підрозділ, лише якщо в ньому немає працівників. */
    public void delete(int departmentId) {
        db.inTransaction(c -> {
            int count = employeeDao.countByDepartment(c, departmentId);
            if (count > 0) {
                throw new ValidationException("У підрозділі є працівники (" + count + "). "
                        + "Спочатку переведіть їх до іншого підрозділу або видаліть.");
            }
            if (!departmentDao.delete(c, departmentId)) {
                throw new ValidationException("Підрозділ не знайдено. Можливо, його вже видалено.");
            }
            return null;
        });
    }

    /**
     * Переводить усіх працівників підрозділу {@code fromId} до підрозділу {@code toId}
     * однією транзакцією.
     *
     * @return кількість переведених працівників
     */
    public int transferAllEmployees(int fromId, int toId) {
        if (fromId == toId) {
            throw new ValidationException("Підрозділ-джерело і підрозділ-призначення мають бути різними.");
        }
        return db.inTransaction(c -> {
            if (departmentDao.findById(c, fromId).isEmpty()) {
                throw new ValidationException("Підрозділ, з якого переводяться працівники, не знайдено.");
            }
            if (departmentDao.findById(c, toId).isEmpty()) {
                throw new ValidationException("Підрозділ, до якого переводяться працівники, не знайдено.");
            }
            int moved = employeeDao.moveAllToDepartment(c, fromId, toId);
            if (moved == 0) {
                throw new ValidationException("У вибраному підрозділі немає працівників для переведення.");
            }
            return moved;
        });
    }

    /** Загальний фонд заробітної плати та частка кожного підрозділу у відсотках. */
    public SalaryFundReport getSalaryFundReport() {
        List<DepartmentFund> funds = db.withConnection(departmentDao::findSalaryFunds);
        return buildReport(funds);
    }

    static SalaryFundReport buildReport(List<DepartmentFund> funds) {
        BigDecimal total = funds.stream()
                .map(DepartmentFund::fund)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<SalaryFundReport.Entry> entries = new ArrayList<>();
        for (DepartmentFund f : funds) {
            BigDecimal share = total.signum() == 0
                    ? BigDecimal.ZERO.setScale(2)
                    : f.fund().multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
            entries.add(new SalaryFundReport.Entry(f.departmentId(), f.departmentName(),
                    f.employeeCount(), f.fund().setScale(2, RoundingMode.HALF_UP), share));
        }
        return new SalaryFundReport(entries, total);
    }

    private void ensureNameIsFree(Connection c, Department department) throws SQLException {
        var existing = departmentDao.findByName(c, department.getName());
        if (existing.isPresent() && existing.get().getId() != department.getId()) {
            throw new ValidationException("Підрозділ з назвою «" + department.getName() + "» вже існує.");
        }
    }
}
