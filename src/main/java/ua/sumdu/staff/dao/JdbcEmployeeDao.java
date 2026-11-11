package ua.sumdu.staff.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ua.sumdu.staff.model.Employee;

/**
 * Реалізація {@link EmployeeDao} засобами JDBC.
 */
public class JdbcEmployeeDao implements EmployeeDao {

    private static final String SELECT_BASE = """
            SELECT e.id, e.last_name, e.first_name, e.middle_name, e.position, e.salary,
                   e.hire_date, e.email, e.department_id, d.name AS department_name
            FROM employees e
            JOIN departments d ON d.id = e.department_id
            """;
    private static final String ORDER = " ORDER BY e.last_name, e.first_name";

    private static final String SELECT_ALL = SELECT_BASE + ORDER;
    private static final String SELECT_BY_DEPARTMENT = SELECT_BASE + " WHERE e.department_id = ?" + ORDER;
    private static final String SELECT_BY_ID = SELECT_BASE + " WHERE e.id = ?";
    private static final String SELECT_BY_EMAIL = SELECT_BASE + " WHERE LOWER(e.email) = LOWER(?)";
    private static final String INSERT = """
            INSERT INTO employees (last_name, first_name, middle_name, position, salary,
                                   hire_date, email, department_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE employees
            SET last_name = ?, first_name = ?, middle_name = ?, position = ?, salary = ?,
                hire_date = ?, email = ?, department_id = ?
            WHERE id = ?
            """;
    private static final String DELETE = "DELETE FROM employees WHERE id = ?";
    private static final String COUNT_BY_DEPARTMENT = "SELECT COUNT(*) FROM employees WHERE department_id = ?";
    private static final String MOVE_ALL = "UPDATE employees SET department_id = ? WHERE department_id = ?";

    @Override
    public List<Employee> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL)) {
            return list(ps);
        }
    }

    @Override
    public List<Employee> findByDepartment(Connection c, int departmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_DEPARTMENT)) {
            ps.setInt(1, departmentId);
            return list(ps);
        }
    }

    @Override
    public Optional<Employee> findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            List<Employee> found = list(ps);
            return found.stream().findFirst();
        }
    }

    @Override
    public Optional<Employee> findByEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email.trim());
            List<Employee> found = list(ps);
            return found.stream().findFirst();
        }
    }

    @Override
    public int insert(Connection c, Employee e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, e);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                e.setId(id);
                return id;
            }
        }
    }

    @Override
    public boolean update(Connection c, Employee e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            fill(ps, e);
            ps.setInt(9, e.getId());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public int countByDepartment(Connection c, int departmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(COUNT_BY_DEPARTMENT)) {
            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int moveAllToDepartment(Connection c, int fromDepartmentId, int toDepartmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(MOVE_ALL)) {
            ps.setInt(1, toDepartmentId);
            ps.setInt(2, fromDepartmentId);
            return ps.executeUpdate();
        }
    }

    /** Параметри 1–8 однакові для INSERT і UPDATE. */
    private void fill(PreparedStatement ps, Employee e) throws SQLException {
        ps.setString(1, e.getLastName());
        ps.setString(2, e.getFirstName());
        setNullableString(ps, 3, e.getMiddleName());
        ps.setString(4, e.getPosition());
        ps.setBigDecimal(5, e.getSalary());
        ps.setDate(6, Date.valueOf(e.getHireDate()));
        setNullableString(ps, 7, e.getEmail());
        ps.setInt(8, e.getDepartmentId());
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private List<Employee> list(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Employee> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee(
                rs.getInt("id"),
                rs.getString("last_name"),
                rs.getString("first_name"),
                rs.getString("middle_name"),
                rs.getString("position"),
                rs.getBigDecimal("salary"),
                rs.getDate("hire_date").toLocalDate(),
                rs.getString("email"),
                rs.getInt("department_id"));
        e.setDepartmentName(rs.getString("department_name"));
        return e;
    }
}
