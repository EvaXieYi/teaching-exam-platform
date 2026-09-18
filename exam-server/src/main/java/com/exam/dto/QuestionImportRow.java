package com.exam.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
/** Excel 导入题目的列，全部按字符串读取，由 QuestionImportService 逐行校验转换。 */
public class QuestionImportRow {
    @ExcelProperty("题型")
    private String questionType;
    @ExcelProperty("题干")
    private String content;
    @ExcelProperty("选项A")
    private String optionA;
    @ExcelProperty("选项B")
    private String optionB;
    @ExcelProperty("选项C")
    private String optionC;
    @ExcelProperty("选项D")
    private String optionD;
    @ExcelProperty("选项E")
    private String optionE;
    @ExcelProperty("选项F")
    private String optionF;
    @ExcelProperty("正确答案")
    private String correctAnswer;
    @ExcelProperty("解析")
    private String analysis;
    @ExcelProperty("难度")
    private String difficulty;
    @ExcelProperty("默认分值")
    private String defaultScore;
    @ExcelProperty("知识点")
    private String knowledgePoints;
    @ExcelProperty("可见范围")
    private String visibility;
}
