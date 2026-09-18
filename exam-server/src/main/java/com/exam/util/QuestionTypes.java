package com.exam.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 题型常量与统一判断。
 * SINGLE/MULTIPLE/JUDGE 为选择题，ESSAY/TERM 为主观题需人工阅卷；导入 Excel 时用 fromLabel 识别中文题型。
 */
public final class QuestionTypes {
    public static final String SINGLE = "SINGLE";
    public static final String MULTIPLE = "MULTIPLE";
    public static final String JUDGE = "JUDGE";
    public static final String FILL = "FILL";
    public static final String ESSAY = "ESSAY";
    public static final String TERM = "TERM";

    /** 展示/导出时的题型顺序。 */
    public static final List<String> ORDER = Collections.unmodifiableList(
            Arrays.asList(SINGLE, MULTIPLE, JUDGE, FILL, ESSAY, TERM));

    private QuestionTypes() {
    }

    /** 题型中文名，未知题型原样返回。 */
    public static String label(String code) {
        if (code == null) {
            return "";
        }
        switch (code.toUpperCase()) {
            case SINGLE:
                return "单选题";
            case MULTIPLE:
                return "多选题";
            case JUDGE:
                return "判断题";
            case FILL:
                return "填空题";
            case ESSAY:
                return "简答题";
            case TERM:
                return "关键词解释题";
            default:
                return code;
        }
    }

    /** 由 code（忽略大小写）或中文别名解析题型，识别不了返回 null。 */
    public static String fromLabel(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replaceAll("\\s+", "");
        if (t.isEmpty()) {
            return null;
        }
        String upper = t.toUpperCase();
        if (isValid(upper)) {
            return upper;
        }
        switch (t) {
            case "单选":
            case "单选题":
                return SINGLE;
            case "多选":
            case "多选题":
                return MULTIPLE;
            case "判断":
            case "判断题":
                return JUDGE;
            case "填空":
            case "填空题":
                return FILL;
            case "简答":
            case "简答题":
                return ESSAY;
            case "关键词解释":
            case "关键词解释题":
            case "名词解释":
                return TERM;
            default:
                return null;
        }
    }

    /** 主观题（简答/关键词解释）需要教师人工阅卷。 */
    public static boolean isSubjective(String code) {
        return ESSAY.equalsIgnoreCase(code) || TERM.equalsIgnoreCase(code);
    }

    /** 选择题：正确答案由选项 isCorrect 汇总。 */
    public static boolean isChoice(String code) {
        return SINGLE.equalsIgnoreCase(code) || MULTIPLE.equalsIgnoreCase(code) || JUDGE.equalsIgnoreCase(code);
    }

    public static boolean isValid(String code) {
        return code != null && ORDER.contains(code.toUpperCase());
    }

    /** 各题型的默认分值。 */
    public static BigDecimal defaultScore(String code) {
        if (code == null) {
            return BigDecimal.ONE;
        }
        switch (code.toUpperCase()) {
            case MULTIPLE:
                return new BigDecimal("4");
            case ESSAY:
                return BigDecimal.TEN;
            case TERM:
                return new BigDecimal("5");
            case SINGLE:
            case JUDGE:
            case FILL:
                return new BigDecimal("2");
            default:
                return BigDecimal.ONE;
        }
    }
}
