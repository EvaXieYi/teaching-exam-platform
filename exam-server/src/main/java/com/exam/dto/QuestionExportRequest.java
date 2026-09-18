package com.exam.dto;

import lombok.Data;

import java.util.List;

@Data
/** 题库导出 PDF：ids 为选中的题目，title 为练习标题，withAnswer 决定是否附答案与解析。 */
public class QuestionExportRequest {
    private List<Long> ids;
    private String title;
    private Boolean withAnswer;
}
