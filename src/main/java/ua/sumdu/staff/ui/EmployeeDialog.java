package ua.sumdu.staff.ui;

import java.time.LocalDate;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.Employee;
import ua.sumdu.staff.service.Validator;

/**
 * Діалог додавання та редагування працівника.
 */
public class EmployeeDialog extends Dialog<Employee> {

    private final TextField lastName = new TextField();
    private final TextField firstName = new TextField();
    private final TextField middleName = new TextField();
    private final TextField position = new TextField();
    private final TextField salary = new TextField();
    private final DatePicker hireDate = new DatePicker();
    private final TextField email = new TextField();
    private final ComboBox<Department> department = new ComboBox<>();

    /**
     * @param source            працівник для редагування або {@code null} для нового
     * @param departments       підрозділи для вибору
     * @param defaultDepartment підрозділ за замовчуванням для нового працівника (може бути {@code null})
     */
    public EmployeeDialog(Employee source, List<Department> departments, Department defaultDepartment) {
        boolean isNew = source == null;
        Employee employee = isNew ? new Employee() : copy(source);

        setTitle(isNew ? "Новий працівник" : "Редагування працівника");
        setHeaderText(isNew ? "Введіть дані нового працівника" : "Змініть дані працівника " + source.getFullName());

        department.getItems().setAll(departments);
        department.setMaxWidth(Double.MAX_VALUE);
        if (isNew) {
            department.setValue(defaultDepartment);
            hireDate.setValue(LocalDate.now());
        } else {
            lastName.setText(employee.getLastName());
            firstName.setText(employee.getFirstName());
            middleName.setText(employee.getMiddleName());
            position.setText(employee.getPosition());
            salary.setText(employee.getSalary() == null ? "" : employee.getSalary().toPlainString());
            hireDate.setValue(employee.getHireDate());
            email.setText(employee.getEmail());
            departments.stream()
                    .filter(d -> d.getId() == employee.getDepartmentId())
                    .findFirst()
                    .ifPresent(department::setValue);
        }
        configureDatePicker();
        salary.setPromptText("наприклад 25000,00");
        middleName.setPromptText("необов'язково");
        email.setPromptText("необов'язково");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Прізвище:"), lastName);
        grid.addRow(1, new Label("Ім'я:"), firstName);
        grid.addRow(2, new Label("По батькові:"), middleName);
        grid.addRow(3, new Label("Посада:"), position);
        grid.addRow(4, new Label("Зарплата, грн:"), salary);
        grid.addRow(5, new Label("Дата прийняття:"), hireDate);
        grid.addRow(6, new Label("Email:"), email);
        grid.addRow(7, new Label("Підрозділ:"), department);
        lastName.setPrefColumnCount(26);
        getDialogPane().setContent(grid);

        ButtonType save = new ButtonType("Зберегти", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(save, cancel);

        Node saveButton = getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                fill(employee);
                Validator.validate(employee);
            } catch (ValidationException ex) {
                Alerts.showError("Перевірте введені дані", ex.getMessage());
                event.consume();
            }
        });

        setResultConverter(button -> button == save ? employee : null);
        Platform.runLater(lastName::requestFocus);
    }

    private void fill(Employee e) {
        e.setLastName(lastName.getText());
        e.setFirstName(firstName.getText());
        e.setMiddleName(middleName.getText());
        e.setPosition(position.getText());
        e.setSalary(Validator.parseSalary(salary.getText()));
        // Користувач міг надрукувати дату, не натиснувши Enter: беремо текст із поля.
        hireDate.setValue(hireDate.getConverter().fromString(hireDate.getEditor().getText()));
        e.setHireDate(hireDate.getValue());
        e.setEmail(email.getText());
        Department d = department.getValue();
        e.setDepartmentId(d == null ? 0 : d.getId());
        e.setDepartmentName(d == null ? null : d.getName());
    }

    private void configureDatePicker() {
        hireDate.setPromptText("дд.мм.рррр");
        hireDate.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.strip(), Formats.DATE);
                } catch (Exception ex) {
                    throw new ValidationException("Дата має бути у форматі дд.мм.рррр, наприклад 01.09.2023.");
                }
            }
        });
    }

    private static Employee copy(Employee s) {
        Employee e = new Employee(s.getId(), s.getLastName(), s.getFirstName(), s.getMiddleName(), s.getPosition(),
                s.getSalary(), s.getHireDate(), s.getEmail(), s.getDepartmentId());
        e.setDepartmentName(s.getDepartmentName());
        return e;
    }
}
