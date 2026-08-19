package com.exam.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
/** 教师给简答题打分。score 不能超过该题在试卷中的满分。 */
public class MarkingRequest {
    private BigDecimal score;
    private String comment;
}
