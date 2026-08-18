package com.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamSaveRequest {
    private Long id;
    private String examName;
    private Long paperId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer allowSubmitMinutes;
    private Integer resultVisible;
    private Integer answerVisible;
    private List<Long> studentIds;
}
