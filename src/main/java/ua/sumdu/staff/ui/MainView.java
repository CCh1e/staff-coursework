package ua.sumdu.staff.ui;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ua.sumdu.staff.AppContext;
import ua.sumdu.staff.model.ImportResult;

/**
 * Головне вікно: меню та вкладки.
 */
public class MainView {

    private final AppContext context;
    private final Stage stage;
    private final Runnable onLogout;

    private final BorderPane root = new BorderPane();
    private final DepartmentsTab departmentsTab;
    private final EmployeesTab employeesTab;
    private final SalaryFundTab salaryFundTab;

    public MainView(AppContext context, Stage stage, Runnable onLogout) {
        this.context = context;
        this.stage = stage;
        this.onLogout = onLogout;
        this.departmentsTab = new DepartmentsTab(context, this::refreshAll);
        this.employeesTab = new EmployeesTab(context, this::refreshAll);
        this.salaryFundTab = new SalaryFundTab(context);

        TabPane tabs = new TabPane(departmentsTab, employeesTab, salaryFundTab);
        root.setTop(buildMenu());
        root.setCenter(tabs);
        refreshAll();
    }

    public Parent getRoot() {
        return root;
    }

    /** Після будь-якої зміни даних оновлюються всі вкладки, щоб вони показували однаковий стан. */
    public void refreshAll() {
        try {
            departmentsTab.refresh();
            employeesTab.refresh();
            salaryFundTab.refresh();
        } catch (RuntimeException e) {
            Alerts.showException(e);
        }
    }

    private MenuBar buildMenu() {
        MenuItem export = new MenuItem("Експорт у JSON…");
        export.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        export.setOnAction(e -> exportJson());

        MenuItem importItem = new MenuItem("Імпорт з JSON…");
        importItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN));
        importItem.setOnAction(e -> importJson());

        MenuItem refresh = new MenuItem("Оновити дані");
        refresh.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        refresh.setOnAction(e -> refreshAll());

        MenuItem logout = new MenuItem("Змінити підключення…");
        logout.setOnAction(e -> onLogout.run());

        MenuItem exit = new MenuItem("Вихід");
        exit.setOnAction(e -> Platform.exit());

        Menu file = new Menu("Файл", null, export, importItem, new SeparatorMenuItem(), refresh,
                new SeparatorMenuItem(), logout, exit);

        MenuItem about = new MenuItem("Про програму");
        about.setOnAction(e -> Alerts.showInfo(StaffApp.TITLE,
                "Курсова робота з дисципліни «Об'єктно-орієнтоване програмування на мові Java».\n"
                        + "Варіант 4.\n\nПідключено: " + context.database().getSettings()));
        Menu help = new Menu("Довідка", null, about);

        return new MenuBar(file, help);
    }

    private void exportJson() {
        FileChooser chooser = jsonChooser("Експорт даних у JSON");
        chooser.setInitialFileName("staff-export-" + LocalDate.now() + ".json");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            int count = context.jsonExchange().exportTo(file.toPath());
            Alerts.showInfo("Експорт виконано", "Збережено підрозділи та працівників (" + count + ") у файл:\n" + file);
        } catch (RuntimeException e) {
            Alerts.showException(e);
        }
    }

    private void importJson() {
        File file = jsonChooser("Імпорт даних з JSON").showOpenDialog(stage);
        if (file == null) {
            return;
        }
        if (!Alerts.confirm("Імпортувати дані з файлу " + file.getName() + "?",
                "Підрозділи з однаковою назвою та працівники з однаковим email буде оновлено, "
                        + "решту записів буде додано.")) {
            return;
        }
        try {
            ImportResult result = context.jsonExchange().importFrom(Path.of(file.getPath()));
            refreshAll();
            Alerts.showInfo("Імпорт виконано", result.toString());
        } catch (RuntimeException e) {
            Alerts.showException(e);
        }
    }

    private static FileChooser jsonChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON-файли (*.json)", "*.json"));
        return chooser;
    }
}
