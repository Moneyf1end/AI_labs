package lab4.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lab4.fuzzy.*;

import java.util.*;

/** Интерфейс лабораторной №4, вариант 1: прогноз вероятности осадков (Мамдани). */
public class FuzzyApp extends Application {

    private final MamdaniEngine engine = WeatherModel.build();
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Map<String, Slider> sliders = new LinkedHashMap<>();
    private final Map<String, XYChart.Series<Number, Number>> markers = new LinkedHashMap<>();
    private final XYChart.Series<Number, Number> aggSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> centroidSeries = new XYChart.Series<>();
    private final Label errorLabel = new Label();
    private final Label resultLabel = new Label("—");
    private final Label interpLabel = new Label();
    private final TextArea rulesArea = new TextArea();
    private boolean syncing = false;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setLeft(buildInputPanel());
        root.setCenter(buildCharts());
        root.setBottom(buildResultPanel());

        stage.setTitle("ЛР №4 — Нечёткий прогноз осадков (Мамдани)");
        stage.setScene(new Scene(root, 1250, 820));
        stage.show();
        calculate(); // начальный расчёт со значениями по умолчанию
    }

    // ---------- панель ввода ----------
    private VBox buildInputPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(0, 15, 0, 0));
        box.setPrefWidth(270);
        Label title = new Label("Входные данные");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        double[] defaults = {15, 80, 1005};
        int i = 0;
        for (FuzzyVar v : engine.inputs.values()) {
            double def = defaults[i++];
            Label l = new Label(v.name + ", " + v.unit + "  [" + (int) v.min + " … " + (int) v.max + "]");
            TextField tf = new TextField(String.valueOf(def));
            Slider s = new Slider(v.min, v.max, def);
            // синхронизация поле <-> ползунок
            s.valueProperty().addListener((o, a, b) -> {
                if (!syncing) { syncing = true; tf.setText(String.format(Locale.US, "%.1f", b.doubleValue())); syncing = false; }
            });
            tf.textProperty().addListener((o, a, b) -> {
                if (syncing) return;
                try {
                    double x = Double.parseDouble(b.trim().replace(',', '.'));
                    if (x >= v.min && x <= v.max) { syncing = true; s.setValue(x); syncing = false; }
                } catch (NumberFormatException ignored) { /* ошибку покажет calculate() */ }
            });
            tf.setOnAction(e -> calculate());
            fields.put(v.name, tf);
            sliders.put(v.name, s);
            box.getChildren().addAll(l, tf, s);
        }

        Button calc = new Button("Рассчитать");
        calc.setDefaultButton(true);
        calc.setMaxWidth(Double.MAX_VALUE);
        calc.setOnAction(e -> calculate());
        Button reset = new Button("Сбросить");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(e -> {
            int k = 0;
            for (FuzzyVar v : engine.inputs.values()) fields.get(v.name).setText(String.valueOf(defaults[k++]));
            calculate();
        });
        errorLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        box.getChildren().addAll(calc, reset, errorLabel);
        return box;
    }

    // ---------- графики ----------
    private GridPane buildCharts() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int idx = 0;
        for (FuzzyVar v : engine.inputs.values()) {
            LineChart<Number, Number> ch = baseChart(v);
            XYChart.Series<Number, Number> m = new XYChart.Series<>();
            m.setName("Текущее значение");
            markers.put(v.name, m);
            ch.getData().add(m);
            grid.add(ch, idx % 2, idx / 2);
            idx++;
        }
        FuzzyVar out = engine.output;
        LineChart<Number, Number> och = baseChart(out);
        aggSeries.setName("Агрегация");
        centroidSeries.setName("Центроид");
        och.getData().addAll(aggSeries, centroidSeries);
        och.setTitle(out.name + " (выход)");
        grid.add(och, 1, 1);
        for (int c = 0; c < 2; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().add(cc);
        }
        return grid;
    }

    private LineChart<Number, Number> baseChart(FuzzyVar v) {
        NumberAxis x = new NumberAxis(v.min, v.max, (v.max - v.min) / 10);
        x.setLabel(v.name + ", " + v.unit);
        NumberAxis y = new NumberAxis(0, 1.05, 0.25);
        y.setLabel("μ");
        LineChart<Number, Number> ch = new LineChart<>(x, y);
        ch.setCreateSymbols(false);
        ch.setAnimated(false);
        ch.setTitle(v.name);
        for (Map.Entry<String, Mf> e : v.terms.entrySet()) {
            XYChart.Series<Number, Number> s = new XYChart.Series<>();
            s.setName(e.getKey());
            Mf f = e.getValue();
            double[][] pts = {{f.a(), 0}, {f.b(), 1}, {f.c(), 1}, {f.d(), 0}};
            double lastX = Double.NaN, lastY = Double.NaN;
            for (int k = 0; k < 4; k++) {
                // пропускаем «вертикальные» края на границе универсума
                if (k == 0 && f.a() == f.b()) continue;
                if (k == 3 && f.c() == f.d()) continue;
                if (pts[k][0] == lastX && pts[k][1] == lastY) continue;
                s.getData().add(new XYChart.Data<>(pts[k][0], pts[k][1]));
                lastX = pts[k][0];
                lastY = pts[k][1];
            }
            ch.getData().add(s);
        }
        return ch;
    }

    // ---------- результат ----------
    private VBox buildResultPanel() {
        resultLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        interpLabel.setStyle("-fx-font-size: 14px;");
        interpLabel.setWrapText(true);
        rulesArea.setEditable(false);
        rulesArea.setPrefRowCount(8);
        rulesArea.setStyle("-fx-font-family: 'Consolas', monospace;");
        VBox box = new VBox(6, resultLabel, interpLabel, new Label("Правила и степени их срабатывания:"), rulesArea);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    // ---------- расчёт ----------
    private void calculate() {
        errorLabel.setText("");
        Map<String, Double> values = new LinkedHashMap<>();
        for (FuzzyVar v : engine.inputs.values()) {
            String txt = fields.get(v.name).getText();
            if (txt == null || txt.isBlank()) { showError("Поле «" + v.name + "» не заполнено."); return; }
            double x;
            try {
                x = Double.parseDouble(txt.trim().replace(',', '.'));
            } catch (NumberFormatException ex) { showError("«" + v.name + "»: «" + txt + "» не является числом."); return; }
            if (Double.isNaN(x) || Double.isInfinite(x) || x < v.min || x > v.max) {
                showError(String.format(Locale.US, "«%s» должно быть в диапазоне от %.0f до %.0f %s.", v.name, v.min, v.max, v.unit));
                return;
            }
            values.put(v.name, x);
        }

        MamdaniEngine.Result r = engine.infer(values);

        // маркеры входных значений
        for (FuzzyVar v : engine.inputs.values()) {
            XYChart.Series<Number, Number> m = markers.get(v.name);
            m.getData().setAll(new XYChart.Data<>(values.get(v.name), 0), new XYChart.Data<>(values.get(v.name), 1));
        }
        // агрегированная функция
        List<XYChart.Data<Number, Number>> agg = new ArrayList<>();
        for (int k = 0; k < r.xs.length; k += 5) agg.add(new XYChart.Data<>(r.xs[k], r.aggregated[k]));
        aggSeries.getData().setAll(agg);

        // текст правил
        StringBuilder sb = new StringBuilder();
        for (FuzzyVar v : engine.inputs.values()) {
            sb.append(String.format(Locale.US, "%s = %.1f %s:  ", v.name, values.get(v.name), v.unit));
            r.fuzzified.get(v.name).forEach((t, mu) -> sb.append(String.format(Locale.US, "%s=%.2f  ", t, mu)));
            sb.append('\n');
        }
        sb.append('\n');
        for (int i = 0; i < engine.rules.size(); i++)
            sb.append(String.format(Locale.US, "R%d [%.2f] %s%n", i + 1, r.activations[i], engine.rules.get(i).text()));
        rulesArea.setText(sb.toString());

        if (Double.isNaN(r.crisp)) {
            centroidSeries.getData().clear();
            resultLabel.setText("Результат не определён");
            interpLabel.setText("Ни одно из правил не сработало для этих значений — прогноз по базе правил невозможен.");
        } else {
            centroidSeries.getData().setAll(new XYChart.Data<>(r.crisp, 0), new XYChart.Data<>(r.crisp, 1));
            resultLabel.setText(String.format(Locale.US, "Вероятность осадков: %.1f %%", r.crisp));
            interpLabel.setText(WeatherModel.category(r.crisp));
        }
    }

    private void showError(String msg) { errorLabel.setText(msg); }

    public static void main(String[] args) { launch(args); }
}
