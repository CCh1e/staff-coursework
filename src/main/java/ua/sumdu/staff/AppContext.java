package ua.sumdu.staff;

import ua.sumdu.staff.dao.DepartmentDao;
import ua.sumdu.staff.dao.EmployeeDao;
import ua.sumdu.staff.dao.JdbcDepartmentDao;
import ua.sumdu.staff.dao.JdbcEmployeeDao;
import ua.sumdu.staff.db.Database;
import ua.sumdu.staff.service.DepartmentService;
import ua.sumdu.staff.service.EmployeeService;
import ua.sumdu.staff.service.JsonExchangeService;

/**
 * Створює та пов'язує між собою об'єкти застосунку після успішного входу.
 */
public final class AppContext {

    private final Database database;
    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final JsonExchangeService jsonExchangeService;

    public AppContext(Database database) {
        this.database = database;
        DepartmentDao departmentDao = new JdbcDepartmentDao();
        EmployeeDao employeeDao = new JdbcEmployeeDao();
        this.departmentService = new DepartmentService(database, departmentDao, employeeDao);
        this.employeeService = new EmployeeService(database, employeeDao, departmentDao);
        this.jsonExchangeService = new JsonExchangeService(database, departmentDao, employeeDao);
    }

    public Database database() {
        return database;
    }

    public DepartmentService departments() {
        return departmentService;
    }

    public EmployeeService employees() {
        return employeeService;
    }

    public JsonExchangeService jsonExchange() {
        return jsonExchangeService;
    }
}
