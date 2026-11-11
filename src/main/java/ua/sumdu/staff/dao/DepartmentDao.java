package ua.sumdu.staff.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.DepartmentFund;
import ua.sumdu.staff.model.DepartmentStats;
import ua.sumdu.staff.model.SortOrder;

/**
 * Доступ до таблиці departments. З'єднання передається ззовні, щоб кілька
 * операцій можна було виконати в одній транзакції.
 */
public interface DepartmentDao {

    List<Department> findAll(Connection c) throws SQLException;

    Optional<Department> findById(Connection c, int id) throws SQLException;

    Optional<Department> findByName(Connection c, String name) throws SQLException;

    /** @return згенерований ідентифікатор */
    int insert(Connection c, Department department) throws SQLException;

    boolean update(Connection c, Department department) throws SQLException;

    boolean delete(Connection c, int id) throws SQLException;

    /** Усі підрозділи з кількістю працівників, відсортовані за цією кількістю. */
    List<DepartmentStats> findAllWithEmployeeCount(Connection c, SortOrder order) throws SQLException;

    /** Сума зарплат працівників кожного підрозділу. */
    List<DepartmentFund> findSalaryFunds(Connection c) throws SQLException;
}
