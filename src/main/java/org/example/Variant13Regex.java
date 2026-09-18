package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Вариант 13: язык a^n b^m (cd)^k,  n>=0, m>=0, k>=1
 */
public class Variant13Regex {

    private static final Pattern LANGUAGE = Pattern.compile("a*b*(cd)+");

    static boolean belongsToLanguage(String word) {
        return LANGUAGE.matcher(word).matches();
    }

    public static void main(String[] args) {
        String[] correct   = {"cd", "acd", "bcd", "abcd", "aabbcd", "cdcdcd", "aabbcdcdcd"};
        String[] incorrect = {"", "a", "ab", "c", "ba", "cda", "cdb", "aabbc"};

        System.out.println("Должны быть приняты:");
        for (String w : correct) {
            System.out.printf("  %-12s -> %s%n", "'" + w + "'", belongsToLanguage(w));
        }

        System.out.println("Должны быть отвергнуты:");
        for (String w : incorrect) {
            System.out.printf("  %-12s -> %s%n", "'" + w + "'", belongsToLanguage(w));
        }

        System.out.println("\nВведите слово для проверки (пустая строка — выход):");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String w = line.trim();
                if (w.equalsIgnoreCase("stop")) break;
                if (w.isEmpty()) break;
                System.out.println(belongsToLanguage(w) ? "принято" : "отвергнуто");
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения ввода: " + e.getMessage());
        }
    }
}