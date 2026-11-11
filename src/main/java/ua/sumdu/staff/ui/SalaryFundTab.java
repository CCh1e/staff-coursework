package ua.sumdu.staff.ui;

import java.math.BigDecimal;
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ua.sumdu.staff.AppContext;
import ua.sumdu.staff.model.SalaryFundReport;
import ua.sumdu.staff.model.SalaryFundReport.Entry;

/**
 * Вкладка «Фонд заробітної плати»: загальний фонд за всіма підрозділами та частка кожного з них.
 */
public class SalaryFundTab extends Tab {

    private final AppContext context;
    private final TableView<Entry> table = new TableView<>();
    private final PieChart chart = new PieChart();
    private final Label total = new Label();

    public SalaryFundTab(AppContext context) {
        super("Фонд заробітної плати");
        this.context = context;
        setClosable(false);
        setContent(build());
    }

    private SplitPane build() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Немає даних"));

        TableColumn<Entry, String> name = new TableColumn<>("Підрозділ");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().departmentName()));
        name.setPrefWidth(220);

        TableColumn<Entry, Integer> count = new TableColumn<>("Працівників");
        count.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().employeeCount()));
        count.setStyle("-fx-alignment: CENTER;");

        TableColumn<Entry, BigDecimal> fund = new TableColumn<>("Фонд, грн");
        fund.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().fund()));
        fund.setCellFactory(col -> EmployeeTable.formatted(Formats::money, "-fx-alignment: CENTER-RIGHT;"));
        fund.setPrefWidth(140);

        TableColumn<Entry, BigDecimal> share = new TableColumn<>("Частка у фонді");
        share.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().sharePercent()));
        share.setCellFactory(col -> EmployeeTable.formatted(Formats::percent, "-fx-alignment: CENTER-RIGHT;"));
        share.setPrefWidth(120);

        table.getColumns().setAll(List.of(name, count, fund, share));

        Label title = new Label("Розподіл фонду заробітної плати за підрозділами");
        title.setFont(Font.font(null, FontWeight.BOLD, 14));
        total.setFont(Font.font(null, FontWeight.BOLD, 14));

        VBox left = new VBox(8, title, table, total);
        VBox.setVgrow(table, Priority.ALWAYS);
        left.setPadding(new Insets(10));

        chart.setTitle("Частка підрозділів у загальному фонді");
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);

        SplitPane split = new SplitPane(left, chart);
        split.setDividerPositions(0.55);
        return split;
    }

    public void refresh() {
        SalaryFundReport report = context.departments().getSalaryFundReport();
        table.getItems().setAll(report.entries());
        total.setText("Загальний фонд заробітної плати: " + Formats.money(report.total()) + " грн");
        chart.getData().setAll(report.entries().stream()
                .filter(e -> e.fund().signum() > 0)
                .map(e -> new PieChart.Data(e.departmentName() + " (" + Formats.percent(e.sharePercent()) + ")",
                        e.fund().doubleValue()))
                .toList());
    }
}
