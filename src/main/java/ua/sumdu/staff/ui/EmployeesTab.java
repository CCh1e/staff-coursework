package ua.sumdu.staff.ui;

import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ua.sumdu.staff.AppContext;
import ua.sumdu.staff.model.Employee;

/**
 * Вкладка «Усі працівники»: перегляд, пошук, додавання, редагування та видалення.
 */
public class EmployeesTab extends Tab {

    private final AppContext context;
    private final Runnable onDataChanged;

    private final ObservableList<Employee> data = FXCollections.observableArrayList();
    private final FilteredList<Employee> filtered = new FilteredList<>(data);
    private final EmployeeTable table = new EmployeeTable(true);
    private final TextField search = new TextField();
    private final Label total = new Label();
    private final Button edit = new Button("Редагувати");
    private final Button delete = new Button("Видалити");

    public EmployeesTab(AppContext context, Runnable onDataChanged) {
        super("Усі працівники");
        this.context = context;
        this.onDataChanged = onDataChanged;
        setClosable(false);
        setContent(build());
    }

    private VBox build() {
        SortedList<Employee> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        search.setPromptText("Пошук за ПІБ, посадою, підрозділом або email");
        search.setPrefColumnCount(32);
        search.textProperty().addListener((obs, old, text) -> applyFilter());

        Button add = new Button("Додати");
        add.setOnAction(e -> add());
        edit.setOnAction(e -> edit());
        delete.setOnAction(e -> delete());
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> updateButtons());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                edit();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, new Label("Пошук:"), search, spacer, add, edit, delete);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(8, toolbar, table, total);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setPadding(new Insets(10));
        updateButtons();
        return root;
    }

    public void refresh() {
        data.setAll(context.employees().getAll());
        applyFilter();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().strip().toLowerCase(Locale.ROOT);
        filtered.setPredicate(e -> q.isEmpty()
                || contains(e.getFullName(), q)
                || contains(e.getPosition(), q)
                || contains(e.getDepartmentName(), q)
                || contains(e.getEmail(), q));
        total.setText("Показано працівників: " + filtered.size() + " з " + data.size());
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void updateButtons() {
        boolean selected = table.getSelectionModel().getSelectedItem() != null;
        edit.setDisable(!selected);
        delete.setDisable(!selected);
    }

    private void add() {
        new EmployeeDialog(null, context.departments().getAll(), null).showAndWait().ifPresent(e -> run(() -> {
            context.employees().add(e);
            onDataChanged.run();
        }));
    }

    private void edit() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        new EmployeeDialog(selected, context.departments().getAll(), null).showAndWait().ifPresent(e -> run(() -> {
            context.employees().update(e);
            onDataChanged.run();
        }));
    }

    private void delete() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && Alerts.confirm("Видалити працівника " + selected.getFullName() + "?",
                "Цю дію не можна скасувати.")) {
            run(() -> {
                context.employees().delete(selected.getId());
                onDataChanged.run();
            });
        }
    }

    private static void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            Alerts.showException(e);
        }
    }
}
