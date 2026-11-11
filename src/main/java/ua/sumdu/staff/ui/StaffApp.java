package ua.sumdu.staff.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ua.sumdu.staff.AppContext;

/**
 * Головний клас JavaFX: спочатку показує вікно входу, після підключення — головне вікно.
 */
public class StaffApp extends Application {

    public static final String TITLE = "Облік підрозділів і працівників";

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // Внутрішні збої JavaFX (наприклад, у модулі спеціальних можливостей Windows)
            // не стосуються дій користувача, тому лише записуються в журнал.
            if (isFromApplicationCode(e)) {
                Alerts.showException(e);
            } else {
                e.printStackTrace();
            }
        });
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginView login = new LoginView(this::showMain);
        stage.setTitle(TITLE + " — підключення до бази даних");
        stage.setScene(new Scene(login.getRoot(), 460, 330));
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private void showMain(AppContext context) {
        MainView main = new MainView(context, stage, this::showLogin);
        stage.setTitle(TITLE + " — " + context.database().getSettings());
        stage.setScene(new Scene(main.getRoot(), 1150, 720));
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.centerOnScreen();
    }

    private static boolean isFromApplicationCode(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (frame.getClassName().startsWith("ua.sumdu.staff.")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
