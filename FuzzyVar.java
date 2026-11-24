package lab4.fuzzy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Лингвистическая переменная: имя, единицы, универсум [min;max] и набор термов. */
public class FuzzyVar {
    public final String name;
    public final String unit;
    public final double min;
    public final double max;
    public final Map<String, Mf> terms = new LinkedHashMap<>();

    public FuzzyVar(String name, String unit, double min, double max) {
        this.name = name;
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    public FuzzyVar term(String termName, Mf mf) {
        terms.put(termName, mf);
        return this;
    }

    /** Фаззификация: степень принадлежности x каждому терму. */
    public Map<String, Double> fuzzify(double x) {
        Map<String, Double> res = new LinkedHashMap<>();
        terms.forEach((n, mf) -> res.put(n, mf.mu(x)));
        return res;
    }
}
