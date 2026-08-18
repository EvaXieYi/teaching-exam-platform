package com.exam.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ScoreCalculator {
    private ScoreCalculator() {
    }

    public static boolean isSubjective(String type) {
        return "ESSAY".equalsIgnoreCase(type);
    }

    public static boolean match(String type, String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return false;
        }
        String student = studentAnswer.trim();
        String correct = correctAnswer == null ? "" : correctAnswer.trim();
        if ("FILL".equalsIgnoreCase(type)) {
            String[] sa = student.split("\\|");
            String[] ca = correct.split("\\|");
            if (sa.length != ca.length) {
                return false;
            }
            for (int i = 0; i < sa.length; i++) {
                if (!sa[i].trim().equalsIgnoreCase(ca[i].trim())) {
                    return false;
                }
            }
            return true;
        }
        if ("MULTIPLE".equalsIgnoreCase(type)) {
            return normalizeMulti(student).equals(normalizeMulti(correct));
        }
        return student.equalsIgnoreCase(correct);
    }

    private static String normalizeMulti(String raw) {
        return Arrays.stream(raw.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.joining(","));
    }
}
