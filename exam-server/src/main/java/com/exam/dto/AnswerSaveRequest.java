package com.exam.dto;

import lombok.Data;

@Data
/** 学生保存某一题的答案和标记。 */
public class AnswerSaveRequest {
    private Long questionId;
    private String studentAnswer;
    private Integer flagged;
}
