package ua.sumdu.staff.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ua.sumdu.staff.model.Employee;

/**
 * Таблиця працівників, що використовується на кількох вкладках.
 */
public class EmployeeTable extends TableView<Employee> {

    public EmployeeTable(boolean showDepartment) {
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPlaceholder(new Label("Немає працівників"));

        getColumns().add(text("ПІБ", Employee::getFullName, 230));
        getColumns().add(text("Посада", Employee::getPosition, 180));
        if (showDepartment) {
            getColumns().add(text("Підрозділ", Employee::getDepartmentName, 150));
        }

        TableColumn<Employee, BigDecimal> salary = new TableColumn<>("Зарплата, грн");
        salary.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSalary()));
        salary.setCellFactory(col -> formatted(Formats::money, "-fx-alignment: CENTER-RIGHT;"));
        salary.setPrefWidth(120);
        getColumns().add(salary);

        TableColumn<Employee, LocalDate> hired = new TableColumn<>("Дата прийняття");
        hired.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getHireDate()));
        hired.setCellFactory(col -> formatted(Formats::date, "-fx-alignment: CENTER;"));
        hired.setPrefWidth(120);
        getColumns().add(hired);

        getColumns().add(text("Email", Employee::getEmail, 200));
    }

    private static TableColumn<Employee, String> text(String title, Function<Employee, String> getter, double width) {
        TableColumn<Employee, String> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new SimpleStringProperty(getter.apply(c.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    /** Комірка, що показує значення у відформатованому вигляді, але сортує за самим значенням. */
    static <S, T> TableCell<S, T> formatted(Function<T, String> format, String style) {
        TableCell<S, T> cell = new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.apply(item));
            }
        };
        cell.setStyle(style);
        return cell;
    }
}
