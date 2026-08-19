package com.exam.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 客观题自动评分。
 * 单选/判断：忽略大小写全等；多选：选项排序后比较；填空：多个空用 | 分隔。
 * 简答题（ESSAY）不算客观题，走人工阅卷。
 */
public final class ScoreCalculator {
    private ScoreCalculator() {
    }

    /** 简答题需要教师人工阅卷。 */
    public static boolean isSubjective(String type) {
        return "ESSAY".equalsIgnoreCase(type);
    }

    /** 客观题是否答对。空答案视为错。 */
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
