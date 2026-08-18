package com.exam.dto;

import lombok.Data;

@Data
public class AnswerSaveRequest {
    private Long questionId;
    private String studentAnswer;
    private Integer flagged;
}
