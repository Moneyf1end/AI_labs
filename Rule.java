package lab4.fuzzy;

import java.util.List;

/** Правило: IF (var1 is term1) AND/OR (var2 is term2) THEN out is outTerm. */
public record Rule(List<String[]> conditions, boolean isAnd, String outTerm) {

    public static Rule and(String outTerm, String... varTermPairs) { return make(true, outTerm, varTermPairs); }

    public static Rule or(String outTerm, String... varTermPairs) { return make(false, outTerm, varTermPairs); }

    private static Rule make(boolean and, String outTerm, String... p) {
        List<String[]> c = new java.util.ArrayList<>();
        for (int i = 0; i < p.length; i += 2) c.add(new String[]{p[i], p[i + 1]});
        return new Rule(c, and, outTerm);
    }

    public String text() {
        StringBuilder sb = new StringBuilder("ЕСЛИ ");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(isAnd ? " И " : " ИЛИ ");
            sb.append(conditions.get(i)[0]).append(" = ").append(conditions.get(i)[1]);
        }
        return sb.append(" ТО осадки = ").append(outTerm).toString();
    }
}
