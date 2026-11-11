package ua.sumdu.staff.ui;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ua.sumdu.staff.AppContext;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.model.DepartmentStats;
import ua.sumdu.staff.model.Employee;
import ua.sumdu.staff.model.SortOrder;

/**
 * Вкладка «Підрозділи»: список підрозділів із кількістю працівників, сортування за цією
 * кількістю, перегляд працівників вибраного підрозділу та переведення всіх працівників.
 */
public class DepartmentsTab extends Tab {

    private final AppContext context;
    private final Runnable onDataChanged;

    private final TableView<DepartmentStats> departments = new TableView<>();
    private final EmployeeTable employees = new EmployeeTable(false);
    private final ComboBox<SortOrder> sortOrder = new ComboBox<>();
    private final Label employeesTitle = new Label();

    private final Button editDepartment = new Button("Редагувати");
    private final Button deleteDepartment = new Button("Видалити");
    private final Button transfer = new Button("Перевести всіх працівників…");
    private final Button addEmployee = new Button("Додати працівника");
    private final Button editEmployee = new Button("Редагувати");
    private final Button deleteEmployee = new Button("Видалити");

    public DepartmentsTab(AppContext context, Runnable onDataChanged) {
        super("Підрозділи");
        this.context = context;
        this.onDataChanged = onDataChanged;
        setClosable(false);
        setContent(build());
    }

    private SplitPane build() {
        configureDepartmentsTable();

        sortOrder.getItems().setAll(SortOrder.values());
        sortOrder.setValue(SortOrder.DESC);
        sortOrder.setOnAction(e -> refresh());

        Button addDepartment = new Button("Додати");
        addDepartment.setOnAction(e -> addDepartment());
        editDepartment.setOnAction(e -> editDepartment());
        deleteDepartment.setOnAction(e -> deleteDepartment());
        transfer.setOnAction(e -> transferEmployees());

        HBox sortBox = new HBox(8, new Label("Сортування за кількістю працівників:"), sortOrder);
        sortBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox depToolbar = new HBox(8, sortBox, spacer, addDepartment, editDepartment, deleteDepartment, transfer);
        depToolbar.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(8, title("Підрозділи та кількість працівників"), depToolbar, departments);
        VBox.setVgrow(departments, Priority.ALWAYS);
        top.setPadding(new Insets(10));

        addEmployee.setOnAction(e -> addEmployee());
        editEmployee.setOnAction(e -> editEmployee());
        deleteEmployee.setOnAction(e -> deleteEmployee());
        employees.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && employees.getSelectionModel().getSelectedItem() != null) {
                editEmployee();
            }
        });
        employeesTitle.setFont(Font.font(null, FontWeight.BOLD, 14));
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox empToolbar = new HBox(8, employeesTitle, spacer2, addEmployee, editEmployee, deleteEmployee);
        empToolbar.setAlignment(Pos.CENTER_LEFT);
        VBox bottom = new VBox(8, empToolbar, employees);
        VBox.setVgrow(employees, Priority.ALWAYS);
        bottom.setPadding(new Insets(10));

        departments.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showEmployees());
        employees.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> updateButtons());

        SplitPane split = new SplitPane(top, new BorderPane(bottom));
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.48);
        updateButtons();
        return split;
    }

    private void configureDepartmentsTable() {
        departments.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        departments.setPlaceholder(new Label("Підрозділів немає"));

        TableColumn<DepartmentStats, String> name = new TableColumn<>("Назва підрозділу");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        name.setPrefWidth(260);

        TableColumn<DepartmentStats, String> location = new TableColumn<>("Розташування");
        location.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().department().getLocation()));
        location.setPrefWidth(200);

        TableColumn<DepartmentStats, String> phone = new TableColumn<>("Телефон");
        phone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().department().getPhone()));
        phone.setPrefWidth(150);

        TableColumn<DepartmentStats, Integer> count = new TableColumn<>("Кількість працівників");
        count.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().employeeCount()));
        count.setStyle("-fx-alignment: CENTER;");
        count.setPrefWidth(170);

        departments.getColumns().setAll(List.of(name, location, phone, count));
        departments.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedDepartment().isPresent()) {
                editDepartment();
            }
        });
    }

    /** Перечитує список підрозділів, зберігаючи вибір. */
    public void refresh() {
        Integer selectedId = selectedDepartment().map(Department::getId).orElse(null);
        List<DepartmentStats> stats = context.departments().getStatistics(sortOrder.getValue());
        departments.getItems().setAll(stats);
        if (selectedId != null) {
            stats.stream().filter(s -> s.id() == selectedId).findFirst()
                    .ifPresent(s -> departments.getSelectionModel().select(s));
        }
        if (departments.getSelectionModel().isEmpty() && !stats.isEmpty()) {
            departments.getSelectionModel().selectFirst();
        }
        showEmployees();
    }

    private void showEmployees() {
        Optional<DepartmentStats> selected = Optional.ofNullable(departments.getSelectionModel().getSelectedItem());
        if (selected.isEmpty()) {
            employeesTitle.setText("Оберіть підрозділ, щоб побачити його працівників");
            employees.getItems().clear();
        } else {
            DepartmentStats s = selected.get();
            employeesTitle.setText("Працівники підрозділу «" + s.name() + "» (" + s.employeeCount() + ")");
            employees.getItems().setAll(context.employees().getByDepartment(s.id()));
        }
        updateButtons();
    }

    private void updateButtons() {
        Optional<DepartmentStats> dep = Optional.ofNullable(departments.getSelectionModel().getSelectedItem());
        boolean hasDepartment = dep.isPresent();
        boolean hasEmployee = employees.getSelectionModel().getSelectedItem() != null;
        editDepartment.setDisable(!hasDepartment);
        deleteDepartment.setDisable(!hasDepartment);
        transfer.setDisable(dep.map(s -> s.employeeCount() == 0).orElse(true));
        addEmployee.setDisable(!hasDepartment);
        editEmployee.setDisable(!hasEmployee);
        deleteEmployee.setDisable(!hasEmployee);
    }

    private Optional<Department> selectedDepartment() {
        return Optional.ofNullable(departments.getSelectionModel().getSelectedItem()).map(DepartmentStats::department);
    }

    private void addDepartment() {
        new DepartmentDialog(null).showAndWait().ifPresent(d -> run(() -> {
            context.departments().add(d);
            onDataChanged.run();
            selectById(d.getId());
        }));
    }

    private void editDepartment() {
        selectedDepartment().ifPresent(selected ->
                new DepartmentDialog(selected).showAndWait().ifPresent(d -> run(() -> {
                    context.departments().update(d);
                    onDataChanged.run();
                })));
    }

    private void deleteDepartment() {
        selectedDepartment().ifPresent(d -> {
            if (Alerts.confirm("Видалити підрозділ «" + d.getName() + "»?", "Цю дію не можна скасувати.")) {
                run(() -> {
                    context.departments().delete(d.getId());
                    onDataChanged.run();
                });
            }
        });
    }

    private void transferEmployees() {
        DepartmentStats from = departments.getSelectionModel().getSelectedItem();
        if (from == null) {
            return;
        }
        List<Department> targets = context.departments().getAll().stream()
                .filter(d -> d.getId() != from.id())
                .toList();
        if (targets.isEmpty()) {
            Alerts.showInfo("Немає куди переводити", "Створіть інший підрозділ, щоб перевести до нього працівників.");
            return;
        }
        ChoiceDialog<Department> dialog = new ChoiceDialog<>(targets.getFirst(), targets);
        dialog.setTitle("Переведення працівників");
        dialog.setHeaderText("Перевести всіх працівників підрозділу «" + from.name() + "» ("
                + from.employeeCount() + ") до іншого підрозділу");
        dialog.setContentText("Підрозділ призначення:");
        ButtonType confirm = new ButtonType("Перевести", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(confirm, Alerts.CANCEL);
        dialog.showAndWait().ifPresent(to -> {
            if (!Alerts.confirm("Перевести " + from.employeeCount() + " працівник(ів)?",
                    "«" + from.name() + "» → «" + to.getName() + "»")) {
                return;
            }
            run(() -> {
                int moved = context.departments().transferAllEmployees(from.id(), to.getId());
                onDataChanged.run();
                selectById(to.getId());
                Alerts.showInfo("Переведення виконано",
                        "До підрозділу «" + to.getName() + "» переведено працівників: " + moved + ".");
            });
        });
    }

    private void addEmployee() {
        List<Department> all = context.departments().getAll();
        Department current = selectedDepartment().orElse(null);
        new EmployeeDialog(null, all, current).showAndWait().ifPresent(e -> run(() -> {
            context.employees().add(e);
            onDataChanged.run();
        }));
    }

    private void editEmployee() {
        Employee selected = employees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        new EmployeeDialog(selected, context.departments().getAll(), null).showAndWait().ifPresent(e -> run(() -> {
            context.employees().update(e);
            onDataChanged.run();
        }));
    }

    private void deleteEmployee() {
        Employee selected = employees.getSelectionModel().getSelectedItem();
        if (selected != null && Alerts.confirm("Видалити працівника " + selected.getFullName() + "?",
                "Цю дію не можна скасувати.")) {
            run(() -> {
                context.employees().delete(selected.getId());
                onDataChanged.run();
            });
        }
    }

    private void selectById(int departmentId) {
        departments.getItems().stream().filter(s -> s.id() == departmentId).findFirst()
                .ifPresent(s -> {
                    departments.getSelectionModel().select(s);
                    departments.scrollTo(s);
                });
    }

    private static Label title(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(null, FontWeight.BOLD, 14));
        return label;
    }

    private static void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            Alerts.showException(e);
        }
    }
}
