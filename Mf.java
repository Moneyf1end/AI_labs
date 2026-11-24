package lab4.fuzzy;

/** Функция принадлежности трапециевидной формы (треугольник — частный случай, когда b == c). */
public record Mf(double a, double b, double c, double d) {

    public static Mf trap(double a, double b, double c, double d) { return new Mf(a, b, c, d); }

    public static Mf tri(double a, double b, double d) { return new Mf(a, b, b, d); }

    /** Степень принадлежности значения x, результат в [0;1]. */
    public double mu(double x) {
        if (x < a || x > d) return 0.0;
        if (x >= b && x <= c) return 1.0;
        if (x < b) return b == a ? 1.0 : (x - a) / (b - a);
        return d == c ? 1.0 : (d - x) / (d - c);
    }
}
