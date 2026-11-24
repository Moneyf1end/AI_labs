# ЛР №4 — Нечёткая система Мамдани (вариант 1: прогноз осадков)

Java 17, JavaFX 21, Maven. Нечёткий вывод реализован вручную, без библиотек.
Подробное описание, теория, правила, графики и тесты — в отчёте `Otchet_LAB4_Mihailovschi.docx`.

## Запуск
```
mvn javafx:run
```
Консольная проверка тестовых сценариев (без JavaFX):
```
mvn compile
java -cp target/classes lab4.fuzzy.Scenarios
```

## Структура
- `src/main/java/lab4/fuzzy` — движок Мамдани (`Mf`, `FuzzyVar`, `Rule`, `MamdaniEngine`), модель погоды `WeatherModel`, сценарии `Scenarios`
- `src/main/java/lab4/ui/FuzzyApp.java` — интерфейс JavaFX
