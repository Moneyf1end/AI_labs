package lab4.fuzzy;

import java.util.*;

/**
 * Нечёткая система Мамдани.
 * 1) фаззификация, 2) AND = min / OR = max, 3) импликация = min (усечение выходного терма),
 * 4) агрегация = max, 5) дефаззификация методом центроида.
 */
public class MamdaniEngine {

    public static final int POINTS = 1001; // шаг дискретизации выходного универсума

    public final Map<String, FuzzyVar> inputs = new LinkedHashMap<>();
    public final FuzzyVar output;
    public final List<Rule> rules = new ArrayList<>();

    public MamdaniEngine(FuzzyVar output) { this.output = output; }

    public MamdaniEngine addInput(FuzzyVar v) { inputs.put(v.name, v); return this; }

    public MamdaniEngine addRule(Rule r) { rules.add(r); return this; }

    /** Результат вывода. crisp == NaN, если ни одно правило не сработало. */
    public static class Result {
        public Map<String, Map<String, Double>> fuzzified = new LinkedHashMap<>();
        public double[] activations;       // степень срабатывания каждого правила
        public double[] xs;                // сетка выходного универсума
        public double[] aggregated;        // агрегированная функция принадлежности
        public double crisp = Double.NaN;  // чёткое значение (центроид)
    }

    public Result infer(Map<String, Double> values) {
        Result r = new Result();

        // 1. Фаззификация
        for (FuzzyVar v : inputs.values()) {
            Double x = values.get(v.name);
            if (x == null) throw new IllegalArgumentException("Не задано значение: " + v.name);
            r.fuzzified.put(v.name, v.fuzzify(x));
        }

        // 2. Активация правил
        r.activations = new double[rules.size()];
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            double act = rule.isAnd() ? 1.0 : 0.0;
            for (String[] c : rule.conditions()) {
                double mu = r.fuzzified.get(c[0]).get(c[1]);
                act = rule.isAnd() ? Math.min(act, mu) : Math.max(act, mu);
            }
            r.activations[i] = act;
        }

        // 3-4. Импликация (min) и агрегация (max)
        r.xs = new double[POINTS];
        r.aggregated = new double[POINTS];
        for (int k = 0; k < POINTS; k++) {
            double x = output.min + (output.max - output.min) * k / (POINTS - 1);
            r.xs[k] = x;
            double agg = 0;
            for (int i = 0; i < rules.size(); i++) {
                double clipped = Math.min(r.activations[i], output.terms.get(rules.get(i).outTerm()).mu(x));
                agg = Math.max(agg, clipped);
            }
            r.aggregated[k] = agg;
        }

        // 5. Центроид: sum(x*mu) / sum(mu)
        double num = 0, den = 0;
        for (int k = 0; k < POINTS; k++) { num += r.xs[k] * r.aggregated[k]; den += r.aggregated[k]; }
        if (den > 1e-12) r.crisp = num / den;
        return r;
    }
}
