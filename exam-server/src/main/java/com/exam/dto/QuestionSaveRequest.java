package com.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionSaveRequest {
    private Long id;
    private Long categoryId;
    private String questionType;
    private String content;
    private String correctAnswer;
    private String analysis;
    private Integer difficulty;
    private BigDecimal defaultScore;
    private String visibility;
    private List<OptionItem> options;
    private List<Long> knowledgePointIds;

    @Data
    public static class OptionItem {
        private String optionKey;
        private String optionContent;
        private Integer isCorrect;
        private Integer sortNo;
    }
}
