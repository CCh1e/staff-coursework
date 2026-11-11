package ua.sumdu.staff.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import ua.sumdu.staff.model.Employee;

/**
 * Доступ до таблиці employees.
 */
public interface EmployeeDao {

    List<Employee> findAll(Connection c) throws SQLException;

    List<Employee> findByDepartment(Connection c, int departmentId) throws SQLException;

    Optional<Employee> findById(Connection c, int id) throws SQLException;

    Optional<Employee> findByEmail(Connection c, String email) throws SQLException;

    int insert(Connection c, Employee employee) throws SQLException;

    boolean update(Connection c, Employee employee) throws SQLException;

    boolean delete(Connection c, int id) throws SQLException;

    int countByDepartment(Connection c, int departmentId) throws SQLException;

    /**
     * Переводить усіх працівників одного підрозділу до іншого.
     *
     * @return кількість переведених працівників
     */
    int moveAllToDepartment(Connection c, int fromDepartmentId, int toDepartmentId) throws SQLException;
}
