package ua.sumdu.staff.ui;

import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import ua.sumdu.staff.exception.AppException;
import ua.sumdu.staff.exception.ValidationException;

/**
 * Стандартні діалогові повідомлення.
 */
public final class Alerts {

    public static final ButtonType YES = new ButtonType("Так", ButtonBar.ButtonData.YES);
    public static final ButtonType NO = new ButtonType("Ні", ButtonBar.ButtonData.NO);
    public static final ButtonType OK = new ButtonType("Гаразд", ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType CANCEL = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);

    private Alerts() {
    }

    public static void showInfo(String header, String text) {
        show(Alert.AlertType.INFORMATION, "Інформація", header, text);
    }

    public static void showError(String header, String text) {
        show(Alert.AlertType.ERROR, "Помилка", header, text);
    }

    public static boolean confirm(String header, String text) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, "Підтвердження", header, text);
        alert.getButtonTypes().setAll(YES, NO);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == YES;
    }

    /** Показує виняток: для помилок застосунку — його повідомлення, для інших — загальний текст. */
    public static void showException(Throwable e) {
        Runnable show = () -> {
            if (e instanceof ValidationException) {
                showError("Перевірте введені дані", e.getMessage());
            } else if (e instanceof AppException) {
                showError("Операцію не виконано", e.getMessage());
            } else {
                e.printStackTrace();
                showError("Неочікувана помилка", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    private static void show(Alert.AlertType type, String title, String header, String text) {
        create(type, title, header, text).showAndWait();
    }

    private static Alert create(Alert.AlertType type, String title, String header, String text) {
        Alert alert = new Alert(type, "", OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Label content = new Label(text);
        content.setWrapText(true);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(480);
        return alert;
    }
}
