package ua.sumdu.staff;

import javafx.application.Application;
import ua.sumdu.staff.ui.StaffApp;

/**
 * Точка входу для виконуваного JAR. Клас не успадковує {@link Application}, інакше
 * JavaFX відмовляється запускатися з class path без модулів.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(StaffApp.class, args);
    }
}
