package ua.sumdu.staff.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.DepartmentFund;
import ua.sumdu.staff.model.DepartmentStats;
import ua.sumdu.staff.model.SortOrder;

/**
 * Реалізація {@link DepartmentDao} засобами JDBC.
 */
public class JdbcDepartmentDao implements DepartmentDao {

    private static final String SELECT_ALL =
            "SELECT id, name, location, phone FROM departments ORDER BY name";
    private static final String SELECT_BY_ID =
            "SELECT id, name, location, phone FROM departments WHERE id = ?";
    private static final String SELECT_BY_NAME =
            "SELECT id, name, location, phone FROM departments WHERE LOWER(name) = LOWER(?)";
    private static final String INSERT =
            "INSERT INTO departments (name, location, phone) VALUES (?, ?, ?)";
    private static final String UPDATE =
            "UPDATE departments SET name = ?, location = ?, phone = ? WHERE id = ?";
    private static final String DELETE =
            "DELETE FROM departments WHERE id = ?";
    // Напрям сортування підставляється з переліку SortOrder, а не з введення користувача.
    private static final String SELECT_WITH_COUNT = """
            SELECT d.id, d.name, d.location, d.phone, COUNT(e.id) AS employee_count
            FROM departments d
            LEFT JOIN employees e ON e.department_id = d.id
            GROUP BY d.id, d.name, d.location, d.phone
            ORDER BY employee_count %s, d.name
            """;
    private static final String SELECT_FUNDS = """
            SELECT d.id, d.name, COUNT(e.id) AS employee_count, COALESCE(SUM(e.salary), 0) AS fund
            FROM departments d
            LEFT JOIN employees e ON e.department_id = d.id
            GROUP BY d.id, d.name
            ORDER BY fund DESC, d.name
            """;

    @Override
    public List<Department> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<Department> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    @Override
    public Optional<Department> findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            return single(ps);
        }
    }

    @Override
    public Optional<Department> findByName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_NAME)) {
            ps.setString(1, name.trim());
            return single(ps);
        }
    }

    @Override
    public int insert(Connection c, Department d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getLocation());
            ps.setString(3, d.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                d.setId(id);
                return id;
            }
        }
    }

    @Override
    public boolean update(Connection c, Department d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getLocation());
            ps.setString(3, d.getPhone());
            ps.setInt(4, d.getId());
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
    public List<DepartmentStats> findAllWithEmployeeCount(Connection c, SortOrder order) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_WITH_COUNT.formatted(order.sql()));
             ResultSet rs = ps.executeQuery()) {
            List<DepartmentStats> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new DepartmentStats(map(rs), rs.getInt("employee_count")));
            }
            return result;
        }
    }

    @Override
    public List<DepartmentFund> findSalaryFunds(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_FUNDS);
             ResultSet rs = ps.executeQuery()) {
            List<DepartmentFund> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new DepartmentFund(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("employee_count"),
                        rs.getBigDecimal("fund")));
            }
            return result;
        }
    }

    private Optional<Department> single(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    private Department map(ResultSet rs) throws SQLException {
        return new Department(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("location"),
                rs.getString("phone"));
    }
}
