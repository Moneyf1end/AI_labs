package lab4.fuzzy;

import java.util.Map;

/** Консольная проверка: 3+ тестовых сценария (запуск: mvn compile exec:java). */
public class Scenarios {
    public static void main(String[] args) {
        MamdaniEngine e = WeatherModel.build();
        double[][] cases = {
                {2, 90, 990},     // холодно, очень влажно, низкое давление -> дождь
                {32, 15, 1030},   // жарко, сухо, высокое давление -> без осадков
                {30, 50, 1013},   // жарко, средняя влажность -> средняя
                {15, 85, 1000},   // умеренно, влажно -> высокая
                {20, 60, 1013},   // пограничный случай
        };
        for (double[] c : cases) {
            var r = e.infer(Map.of(WeatherModel.T, c[0], WeatherModel.H, c[1], WeatherModel.P, c[2]));
            System.out.printf("T=%.0f°C H=%.0f%% P=%.0f гПа -> %s%n", c[0], c[1], c[2],
                    Double.isNaN(r.crisp) ? "правила не сработали" : String.format("%.1f%%", r.crisp));
        }
    }
}
