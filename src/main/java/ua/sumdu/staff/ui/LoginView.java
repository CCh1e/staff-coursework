package ua.sumdu.staff.ui;

import java.util.function.Consumer;

import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ua.sumdu.staff.AppContext;
import ua.sumdu.staff.db.ConnectionSettings;
import ua.sumdu.staff.db.Database;
import ua.sumdu.staff.exception.AppException;

/**
 * Вікно входу: параметри сервера, логін і пароль користувача PostgreSQL.
 */
public class LoginView {

    private final VBox root = new VBox(14);
    private final TextField host = new TextField("localhost");
    private final TextField port = new TextField("5432");
    private final TextField database = new TextField("staff_db");
    private final TextField user = new TextField("postgres");
    private final PasswordField password = new PasswordField();
    private final Button connect = new Button("Підключитися");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label status = new Label();

    private final Consumer<AppContext> onConnected;

    public LoginView(Consumer<AppContext> onConnected) {
        this.onConnected = onConnected;
        build();
    }

    public Parent getRoot() {
        return root;
    }

    private void build() {
        Label title = new Label("Підключення до бази даних");
        title.setFont(Font.font(null, FontWeight.BOLD, 18));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.RIGHT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);

        form.addRow(0, new Label("Сервер:"), host);
        form.addRow(1, new Label("Порт:"), port);
        form.addRow(2, new Label("База даних:"), database);
        form.addRow(3, new Label("Користувач:"), user);
        form.addRow(4, new Label("Пароль:"), password);

        progress.setVisible(false);
        progress.setPrefSize(24, 24);
        connect.setDefaultButton(true);
        connect.setOnAction(e -> connect());
        HBox buttons = new HBox(10, progress, connect);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        status.setWrapText(true);
        status.setStyle("-fx-text-fill: #b00020;");

        root.setPadding(new Insets(24));
        root.getChildren().addAll(title, form, buttons, status);
        password.requestFocus();
    }

    private void connect() {
        ConnectionSettings settings;
        try {
            settings = readSettings();
        } catch (AppException e) {
            status.setText(e.getMessage());
            return;
        }

        Database db = new Database(settings);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                db.checkConnection();
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            onConnected.accept(new AppContext(db));
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            status.setText(error instanceof AppException ? error.getMessage() : "Помилка: " + error.getMessage());
        });
        setBusy(true);
        status.setText("");
        Thread thread = new Thread(task, "db-connect");
        thread.setDaemon(true);
        thread.start();
    }

    private ConnectionSettings readSettings() {
        String h = host.getText().strip();
        String db = database.getText().strip();
        String u = user.getText().strip();
        if (h.isEmpty() || db.isEmpty() || u.isEmpty()) {
            throw new AppException("Заповніть поля «Сервер», «База даних» і «Користувач».");
        }
        int p;
        try {
            p = Integer.parseInt(port.getText().strip());
        } catch (NumberFormatException e) {
            throw new AppException("Порт має бути цілим числом (зазвичай 5432).");
        }
        if (p < 1 || p > 65535) {
            throw new AppException("Порт має бути в межах 1–65535.");
        }
        return new ConnectionSettings(h, p, db, u, password.getText());
    }

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        connect.setDisable(busy);
    }
}
