package com.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
/** 组卷请求：试卷名、及格分、题目及每题分值。 */
public class PaperSaveRequest {
    private Long id;
    private String paperName;
    private BigDecimal passScore;
    private List<Item> questions;

    @Data
    public static class Item {
        private Long questionId;
        private BigDecimal questionScore;
        private Integer sortNo;
    }
}
