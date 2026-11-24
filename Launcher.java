package lab4.ui;

/**
 * Запускающий класс-обёртка. Он НЕ наследует javafx.application.Application,
 * поэтому его можно запускать обычной зелёной кнопкой в IntelliJ:
 * JavaFX берётся из Maven-зависимостей, а не из JDK.
 */
public class Launcher {
    public static void main(String[] args) {
        FuzzyApp.main(args);
    }
}
