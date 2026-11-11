package ua.sumdu.staff.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import ua.sumdu.staff.exception.ValidationException;
import ua.sumdu.staff.model.Department;
import ua.sumdu.staff.service.Validator;

/**
 * Діалог додавання та редагування підрозділу.
 */
public class DepartmentDialog extends Dialog<Department> {

    private final TextField name = new TextField();
    private final TextField location = new TextField();
    private final TextField phone = new TextField();

    public DepartmentDialog(Department source) {
        boolean isNew = source == null;
        Department department = isNew ? new Department() : copy(source);

        setTitle(isNew ? "Новий підрозділ" : "Редагування підрозділу");
        setHeaderText(isNew ? "Введіть дані нового підрозділу" : "Змініть дані підрозділу");

        name.setText(department.getName());
        location.setText(department.getLocation());
        phone.setText(department.getPhone());
        name.setPromptText("обов'язково");
        phone.setPromptText("+380...");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Назва:"), name);
        grid.addRow(1, new Label("Розташування:"), location);
        grid.addRow(2, new Label("Телефон:"), phone);
        name.setPrefColumnCount(28);
        getDialogPane().setContent(grid);

        ButtonType save = new ButtonType("Зберегти", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(save, cancel);

        // Діалог не закривається, поки дані не пройдуть перевірку.
        Node saveButton = getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            department.setName(name.getText());
            department.setLocation(location.getText());
            department.setPhone(phone.getText());
            try {
                Validator.validate(department);
            } catch (ValidationException ex) {
                Alerts.showError("Перевірте введені дані", ex.getMessage());
                event.consume();
            }
        });

        setResultConverter(button -> button == save ? department : null);
        Platform.runLater(name::requestFocus);
    }

    private static Department copy(Department d) {
        return new Department(d.getId(), d.getName(), d.getLocation(), d.getPhone());
    }
}
