package ua.sumdu.staff.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ua.sumdu.staff.dao.DepartmentDao;
import ua.sumdu.staff.dao.EmployeeDao;
import ua.sumdu.staff.db.Database;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Employee;

/**
 * Операції з працівниками.
 */
public class EmployeeService {

    private final Database db;
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;

    public EmployeeService(Database db, EmployeeDao employeeDao, DepartmentDao departmentDao) {
        this.db = db;
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
    }

    public List<Employee> getAll() {
        return db.withConnection(employeeDao::findAll);
    }

    public List<Employee> getByDepartment(int departmentId) {
        return db.withConnection(c -> employeeDao.findByDepartment(c, departmentId));
    }

    public Employee add(Employee employee) {
        Validator.validate(employee);
        return db.inTransaction(c -> {
            checkReferences(c, employee);
            employeeDao.insert(c, employee);
            return employee;
        });
    }

    public void update(Employee employee) {
        Validator.validate(employee);
        db.inTransaction(c -> {
            checkReferences(c, employee);
            if (!employeeDao.update(c, employee)) {
                throw new ValidationException("Працівника не знайдено. Можливо, його вже видалено.");
            }
            return null;
        });
    }

    public void delete(int employeeId) {
        db.withConnection(c -> {
            if (!employeeDao.delete(c, employeeId)) {
                throw new ValidationException("Працівника не знайдено. Можливо, його вже видалено.");
            }
            return null;
        });
    }

    private void checkReferences(Connection c, Employee employee) throws SQLException {
        if (departmentDao.findById(c, employee.getDepartmentId()).isEmpty()) {
            throw new ValidationException("Вибраний підрозділ не існує.");
        }
        if (employee.getEmail() != null) {
            var sameEmail = employeeDao.findByEmail(c, employee.getEmail());
            if (sameEmail.isPresent() && sameEmail.get().getId() != employee.getId()) {
                throw new ValidationException("Email «" + employee.getEmail() + "» вже використовується працівником "
                        + sameEmail.get().getFullName() + ".");
            }
        }
    }
}
