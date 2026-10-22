package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Лабораторная работа, вариант №1: моделирование стаи птиц (Boids, Крейг Рейнольдс).
 *
 * Запуск с окном:        java Boids
 * Запуск экспериментов:  java Boids --experiments
 */
public class Boids {

    /** Параметры среды, агентов и правил поведения — всё меняется в одном месте. */
    static class Params {
        // среда
        double width = 100, height = 100;
        boolean wrap = true;          // true: циклические границы, false: отражение
        // агенты
        int count = 40;
        double maxSpeed = 2.0;
        double minSpeed = 0.5;
        // правила
        double radius = 15;           // радиус взаимодействия
        double sepRadius = 4;         // дистанция, ближе которой работает разделение
        double wCohesion = 0.01;
        double wAlignment = 0.05;
        double wSeparation = 0.1;

        Params copy() {
            Params p = new Params();
            p.width = width; p.height = height; p.wrap = wrap; p.count = count;
            p.maxSpeed = maxSpeed; p.minSpeed = minSpeed; p.radius = radius;
            p.sepRadius = sepRadius; p.wCohesion = wCohesion;
            p.wAlignment = wAlignment; p.wSeparation = wSeparation;
            return p;
        }
    }

    /** Модель: массивы позиций и скоростей агентов + один шаг симуляции. */
    static class Sim {
        final Params p;
        final long seed;
        Random rnd;
        double[] x, y, vx, vy;
        int step = 0;

        Sim(Params p, long seed) { this.p = p; this.seed = seed; reset(); }

        void reset() {
            rnd = new Random(seed);
            int n = p.count;
            x = new double[n]; y = new double[n]; vx = new double[n]; vy = new double[n];
            for (int i = 0; i < n; i++) {
                x[i] = rnd.nextDouble() * p.width;
                y[i] = rnd.nextDouble() * p.height;
                double a = rnd.nextDouble() * 2 * Math.PI;
                double s = p.minSpeed + rnd.nextDouble() * (p.maxSpeed - p.minSpeed);
                vx[i] = Math.cos(a) * s;
                vy[i] = Math.sin(a) * s;
            }
            step = 0;
        }

        /** Разность координат с учётом циклической границы. */
        double dx(double a, double b) {
            double d = b - a;
            if (p.wrap) { if (d > p.width / 2) d -= p.width; else if (d < -p.width / 2) d += p.width; }
            return d;
        }
        double dy(double a, double b) {
            double d = b - a;
            if (p.wrap) { if (d > p.height / 2) d -= p.height; else if (d < -p.height / 2) d += p.height; }
            return d;
        }

        void step() {
            int n = x.length;
            double[] nvx = new double[n], nvy = new double[n];
            double r2 = p.radius * p.radius, s2 = p.sepRadius * p.sepRadius;
            for (int i = 0; i < n; i++) {
                double cx = 0, cy = 0, ax = 0, ay = 0, sx = 0, sy = 0;
                int cnt = 0;
                for (int j = 0; j < n; j++) {
                    if (j == i) continue;
                    double ddx = dx(x[i], x[j]), ddy = dy(y[i], y[j]);
                    double d2 = ddx * ddx + ddy * ddy;
                    if (d2 < r2) {
                        cnt++;
                        cx += ddx; cy += ddy;          // смещение к соседу (для центра масс)
                        ax += vx[j]; ay += vy[j];
                        if (d2 < s2) { sx -= ddx; sy -= ddy; } // отталкивание от близких
                    }
                }
                double newVx = vx[i], newVy = vy[i];
                if (cnt > 0) {
                    // центрирование: вектор к центру масс соседей
                    newVx += p.wCohesion * (cx / cnt);
                    newVy += p.wCohesion * (cy / cnt);
                    // выравнивание: разница со средней скоростью соседей
                    newVx += p.wAlignment * (ax / cnt - vx[i]);
                    newVy += p.wAlignment * (ay / cnt - vy[i]);
                }
                // разделение
                newVx += p.wSeparation * sx;
                newVy += p.wSeparation * sy;
                // ограничение скорости
                double sp = Math.hypot(newVx, newVy);
                if (sp > p.maxSpeed) { newVx *= p.maxSpeed / sp; newVy *= p.maxSpeed / sp; }
                else if (sp < p.minSpeed && sp > 1e-9) { newVx *= p.minSpeed / sp; newVy *= p.minSpeed / sp; }
                nvx[i] = newVx; nvy[i] = newVy;
            }
            // синхронное обновление + границы
            for (int i = 0; i < n; i++) {
                vx[i] = nvx[i]; vy[i] = nvy[i];
                x[i] += vx[i]; y[i] += vy[i];
                if (p.wrap) {
                    x[i] = ((x[i] % p.width) + p.width) % p.width;
                    y[i] = ((y[i] % p.height) + p.height) % p.height;
                } else {
                    if (x[i] < 0) { x[i] = 0; vx[i] = Math.abs(vx[i]); }
                    if (x[i] > p.width) { x[i] = p.width; vx[i] = -Math.abs(vx[i]); }
                    if (y[i] < 0) { y[i] = 0; vy[i] = Math.abs(vy[i]); }
                    if (y[i] > p.height) { y[i] = p.height; vy[i] = -Math.abs(vy[i]); }
                }
            }
            step++;
        }

        // ---------- метрики ----------

        /** Поляризация: 1 — все летят в одну сторону, ~0 — хаос. */
        double polarization() {
            double sx = 0, sy = 0;
            for (int i = 0; i < x.length; i++) {
                double s = Math.hypot(vx[i], vy[i]);
                if (s > 1e-9) { sx += vx[i] / s; sy += vy[i] / s; }
            }
            return Math.hypot(sx, sy) / x.length;
        }

        /** Среднее число соседей в радиусе взаимодействия. */
        double avgNeighbors() {
            int n = x.length; long total = 0; double r2 = p.radius * p.radius;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    double a = dx(x[i], x[j]), b = dy(y[i], y[j]);
                    if (a * a + b * b < r2) total += 2;
                }
            return (double) total / n;
        }

        /** Среднее расстояние до ближайшего соседа. */
        double avgNearest() {
            int n = x.length; double sum = 0;
            for (int i = 0; i < n; i++) {
                double best = Double.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    best = Math.min(best, Math.hypot(dx(x[i], x[j]), dy(y[i], y[j])));
                }
                sum += best;
            }
            return sum / n;
        }

        /** Число пар агентов ближе, чем collisionDist (условные столкновения). */
        int collisions(double collisionDist) {
            int n = x.length, c = 0;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    if (Math.hypot(dx(x[i], x[j]), dy(y[i], y[j])) < collisionDist) c++;
            return c;
        }

        /** Число групп: связные компоненты графа «расстояние < радиуса». */
        int clusters() {
            int n = x.length;
            int[] parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    if (Math.hypot(dx(x[i], x[j]), dy(y[i], y[j])) < p.radius) {
                        int a = find(parent, i), b = find(parent, j);
                        if (a != b) parent[a] = b;
                    }
            int c = 0;
            for (int i = 0; i < n; i++) if (find(parent, i) == i) c++;
            return c;
        }
        private static int find(int[] p, int i) { while (p[i] != i) { p[i] = p[p[i]]; i = p[i]; } return i; }
    }

    // ================= Визуализация =================

    static class View extends JPanel {
        final Sim sim;
        boolean trails = false;
        final List<double[]> history = new ArrayList<>(); // позиции прошлых шагов

        View(Sim sim) { this.sim = sim; setPreferredSize(new Dimension(700, 700)); setBackground(new Color(18, 24, 38)); }

        void clearTrails() { history.clear(); }

        void remember() {
            if (!trails) return;
            double[] snap = new double[sim.x.length * 2];
            for (int i = 0; i < sim.x.length; i++) { snap[2 * i] = sim.x[i]; snap[2 * i + 1] = sim.y[i]; }
            history.add(snap);
            if (history.size() > 25) history.remove(0);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double sx = getWidth() / sim.p.width, sy = getHeight() / sim.p.height;
            if (trails) {
                g.setStroke(new BasicStroke(1f));
                for (int h = 1; h < history.size(); h++) {
                    int alpha = 20 + 80 * h / history.size();
                    g.setColor(new Color(120, 180, 255, alpha));
                    double[] a = history.get(h - 1), b = history.get(h);
                    for (int i = 0; i < b.length / 2; i++) {
                        if (Math.abs(a[2 * i] - b[2 * i]) > sim.p.width / 2 || Math.abs(a[2 * i + 1] - b[2 * i + 1]) > sim.p.height / 2) continue;
                        g.drawLine((int) (a[2 * i] * sx), (int) (a[2 * i + 1] * sy), (int) (b[2 * i] * sx), (int) (b[2 * i + 1] * sy));
                    }
                }
            }
            for (int i = 0; i < sim.x.length; i++) {
                double ang = Math.atan2(sim.vy[i], sim.vx[i]);
                double px = sim.x[i] * sx, py = sim.y[i] * sy;
                Path2D.Double t = new Path2D.Double();
                t.moveTo(px + Math.cos(ang) * 9, py + Math.sin(ang) * 9);
                t.lineTo(px + Math.cos(ang + 2.5) * 6, py + Math.sin(ang + 2.5) * 6);
                t.lineTo(px + Math.cos(ang - 2.5) * 6, py + Math.sin(ang - 2.5) * 6);
                t.closePath();
                float hue = (float) ((ang / (2 * Math.PI) + 1) % 1.0);
                g.setColor(Color.getHSBColor(hue, 0.6f, 1f));
                g.fill(t);
            }
            g.setColor(Color.WHITE);
            g.drawString(String.format("шаг %d | поляризация %.2f | ср. соседей %.1f | групп %d | столкновений %d",
                    sim.step, sim.polarization(), sim.avgNeighbors(), sim.clusters(), sim.collisions(1.0)), 10, 18);
        }
    }

    static JPanel slider(String name, double min, double max, DoubleSupplier get, DoubleConsumer set) {
        JPanel row = new JPanel(new BorderLayout());
        JLabel label = new JLabel();
        int steps = 1000;
        JSlider s = new JSlider(0, steps, (int) ((get.getAsDouble() - min) / (max - min) * steps));
        Runnable upd = () -> label.setText(String.format("%s: %.3f", name, get.getAsDouble()));
        s.addChangeListener(e -> { set.accept(min + (max - min) * s.getValue() / steps); upd.run(); });
        upd.run();
        row.add(label, BorderLayout.NORTH);
        row.add(s, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return row;
    }

    static void runGui() {
        Params p = new Params();
        Sim[] simRef = { new Sim(p, 42) };
        // Sim хранит ссылку на p, поэтому изменения ползунков действуют сразу
        View view = new View(simRef[0]);
        JFrame f = new JFrame("Boids — стая птиц (вариант 1)");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(260, 700));
        side.add(slider("Радиус взаимодействия", 2, 50, () -> p.radius, v -> p.radius = v));
        side.add(slider("Радиус разделения", 1, 15, () -> p.sepRadius, v -> p.sepRadius = v));
        side.add(slider("Вес центрирования", 0, 0.1, () -> p.wCohesion, v -> p.wCohesion = v));
        side.add(slider("Вес выравнивания", 0, 0.3, () -> p.wAlignment, v -> p.wAlignment = v));
        side.add(slider("Вес разделения", 0, 0.5, () -> p.wSeparation, v -> p.wSeparation = v));
        side.add(slider("Макс. скорость", 0.5, 5, () -> p.maxSpeed, v -> p.maxSpeed = v));
        side.add(slider("Число птиц (после «Сброс»)", 20, 200, () -> p.count, v -> p.count = (int) v));

        JCheckBox wrap = new JCheckBox("Циклические границы (иначе отражение)", true);
        wrap.addActionListener(e -> p.wrap = wrap.isSelected());
        JCheckBox trails = new JCheckBox("Следы траекторий");
        trails.addActionListener(e -> { view.trails = trails.isSelected(); view.clearTrails(); });
        JToggleButton pause = new JToggleButton("Пауза");
        JButton reset = new JButton("Сброс");
        reset.addActionListener(e -> { simRef[0].reset(); view.clearTrails(); });
        side.add(wrap); side.add(trails); side.add(pause); side.add(reset);

        f.add(view, BorderLayout.CENTER);
        f.add(side, BorderLayout.EAST);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        new javax.swing.Timer(30, e -> {
            if (!pause.isSelected()) { simRef[0].step(); view.remember(); }
            view.repaint();
        }).start();
    }

    // ================= Эксперименты (без окна) =================

    static void runExperiments() {
        final int STEPS = 600, TAIL = 200;
        List<String> names = new ArrayList<>();
        List<Params> list = new ArrayList<>();
        add(names, list, "Базовый (R=15; 0.01/0.05/0.1)", p -> {});
        add(names, list, "R=5", p -> p.radius = 5);
        add(names, list, "R=30", p -> p.radius = 30);
        add(names, list, "R=50", p -> p.radius = 50);
        add(names, list, "Без центрирования (wCoh=0)", p -> p.wCohesion = 0);
        add(names, list, "Сильное центрирование (wCoh=0.05)", p -> p.wCohesion = 0.05);
        add(names, list, "Без выравнивания (wAli=0)", p -> p.wAlignment = 0);
        add(names, list, "Сильное выравнивание (wAli=0.2)", p -> p.wAlignment = 0.2);
        add(names, list, "Без разделения (wSep=0)", p -> p.wSeparation = 0);
        add(names, list, "Сильное разделение (wSep=0.4)", p -> p.wSeparation = 0.4);
        add(names, list, "Отражение от границ", p -> p.wrap = false);

        System.out.printf("Агентов: 40, шагов: %d, усреднение по последним %d шагам, seed=42%n%n", STEPS, TAIL);
        System.out.println("| Эксперимент | R | wCoh | wAli | wSep | Поляризация | Ср. соседей | Ср. расст. до ближ. | Групп | Столкн. (пар/шаг) |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|");
        for (int k = 0; k < list.size(); k++) {
            Params p = list.get(k);
            Sim s = new Sim(p, 42);
            double pol = 0, nb = 0, near = 0, cl = 0, col = 0;
            for (int t = 0; t < STEPS; t++) {
                s.step();
                if (t >= STEPS - TAIL) {
                    pol += s.polarization(); nb += s.avgNeighbors(); near += s.avgNearest();
                    cl += s.clusters(); col += s.collisions(1.0);
                }
            }
            System.out.printf(Locale.US, "| %s | %.0f | %.2f | %.2f | %.2f | %.2f | %.1f | %.2f | %.1f | %.2f |%n",
                    names.get(k), p.radius, p.wCohesion, p.wAlignment, p.wSeparation,
                    pol / TAIL, nb / TAIL, near / TAIL, cl / TAIL, col / TAIL);
        }
    }

    static void add(List<String> names, List<Params> list, String name, java.util.function.Consumer<Params> mod) {
        Params p = new Params();
        mod.accept(p);
        names.add(name); list.add(p);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--experiments")) runExperiments();
        else SwingUtilities.invokeLater(Boids::runGui);
    }
}