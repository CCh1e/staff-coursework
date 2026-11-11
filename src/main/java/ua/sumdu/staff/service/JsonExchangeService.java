package ua.sumdu.staff.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ua.sumdu.staff.dao.DepartmentDao;
import ua.sumdu.staff.dao.EmployeeDao;
import ua.sumdu.staff.db.Database;
import ua.sumdu.staff.exception.DataAccessException;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.json.ExchangeData;
import ua.sumdu.staff.json.ExchangeData.DepartmentDto;
import ua.sumdu.staff.json.ExchangeData.EmployeeDto;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.Employee;
import ua.sumdu.staff.model.ImportResult;

/**
 * Експорт даних про працівників і підрозділи у JSON-файл та імпорт із нього.
 */
public class JsonExchangeService {

    private final Database db;
    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;
    private final ObjectMapper mapper;

    public JsonExchangeService(Database db, DepartmentDao departmentDao, EmployeeDao employeeDao) {
        this.db = db;
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** @return кількість експортованих працівників */
    public int exportTo(Path file) {
        ExchangeData data = db.withConnection(c -> {
            List<DepartmentDto> departments = departmentDao.findAll(c).stream()
                    .map(d -> new DepartmentDto(d.getName(), d.getLocation(), d.getPhone()))
                    .toList();
            List<EmployeeDto> employees = employeeDao.findAll(c).stream()
                    .map(e -> new EmployeeDto(e.getLastName(), e.getFirstName(), e.getMiddleName(),
                            e.getPosition(), e.getSalary(), e.getHireDate(), e.getEmail(), e.getDepartmentName()))
                    .toList();
            return new ExchangeData(ExchangeData.CURRENT_VERSION,
                    LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), departments, employees);
        });
        try {
            mapper.writeValue(file.toFile(), data);
        } catch (IOException e) {
            throw new DataAccessException("Не вдалося записати файл " + file.getFileName() + ": " + e.getMessage(), e);
        }
        return data.employees().size();
    }

    /**
     * Імпортує дані однією транзакцією. Підрозділи зіставляються за назвою, працівники — за email:
     * наявні записи оновлюються, нові додаються. Якщо хоча б один запис некоректний,
     * база даних не змінюється.
     */
    public ImportResult importFrom(Path file) {
        ExchangeData data = read(file);
        List<Department> departments = toDepartments(data);
        List<EmployeeDto> employees = data.employees() == null ? List.of() : data.employees();

        return db.inTransaction(c -> {
            int depAdded = 0;
            int depUpdated = 0;
            Map<String, Integer> idsByName = new HashMap<>();
            for (Department d : departments) {
                Optional<Department> existing = departmentDao.findByName(c, d.getName());
                if (existing.isPresent()) {
                    d.setId(existing.get().getId());
                    departmentDao.update(c, d);
                    depUpdated++;
                } else {
                    departmentDao.insert(c, d);
                    depAdded++;
                }
                idsByName.put(key(d.getName()), d.getId());
            }

            int empAdded = 0;
            int empUpdated = 0;
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < employees.size(); i++) {
                EmployeeDto dto = employees.get(i);
                String label = "Працівник №" + (i + 1) + ": ";
                Integer departmentId = dto.department() == null ? null : idsByName.get(key(dto.department()));
                if (departmentId == null && dto.department() != null) {
                    departmentId = departmentDao.findByName(c, dto.department()).map(Department::getId).orElse(null);
                }
                if (departmentId == null) {
                    errors.add(label + "підрозділ «" + dto.department() + "» не знайдено ні у файлі, ні в базі.");
                    continue;
                }
                Employee e = new Employee(0, dto.lastName(), dto.firstName(), dto.middleName(), dto.position(),
                        dto.salary(), dto.hireDate(), dto.email(), departmentId);
                try {
                    Validator.validate(e);
                } catch (ValidationException ex) {
                    errors.add(label + String.join(" ", ex.getErrors()));
                    continue;
                }
                Optional<Employee> existing = e.getEmail() == null
                        ? Optional.empty()
                        : employeeDao.findByEmail(c, e.getEmail());
                if (existing.isPresent()) {
                    e.setId(existing.get().getId());
                    employeeDao.update(c, e);
                    empUpdated++;
                } else {
                    employeeDao.insert(c, e);
                    empAdded++;
                }
            }
            if (!errors.isEmpty()) {
                errors.addFirst("Імпорт скасовано, базу даних не змінено. Виправте помилки у файлі:");
                throw new ValidationException(errors);
            }
            return new ImportResult(depAdded, depUpdated, empAdded, empUpdated);
        });
    }

    private ExchangeData read(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new ValidationException("Файл " + file + " не знайдено.");
        }
        try {
            ExchangeData data = mapper.readValue(file.toFile(), ExchangeData.class);
            if (data == null || (data.departments() == null && data.employees() == null)) {
                throw new ValidationException("Файл не містить даних про підрозділи чи працівників.");
            }
            return data;
        } catch (JacksonException e) {
            throw new ValidationException("Файл має некоректний формат JSON: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new DataAccessException("Не вдалося прочитати файл " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private List<Department> toDepartments(ExchangeData data) {
        List<Department> result = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<DepartmentDto> dtos = data.departments() == null ? List.of() : data.departments();
        for (int i = 0; i < dtos.size(); i++) {
            DepartmentDto dto = dtos.get(i);
            Department d = new Department(0, dto.name(), dto.location(), dto.phone());
            try {
                Validator.validate(d);
                result.add(d);
            } catch (ValidationException ex) {
                errors.add("Підрозділ №" + (i + 1) + ": " + String.join(" ", ex.getErrors()));
            }
        }
        if (!errors.isEmpty()) {
            errors.addFirst("Імпорт скасовано, базу даних не змінено. Виправте помилки у файлі:");
            throw new ValidationException(errors);
        }
        return result;
    }

    private static String key(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
