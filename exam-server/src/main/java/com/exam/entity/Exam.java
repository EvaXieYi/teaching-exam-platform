package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam")
/** 一场真实考试：哪份试卷、何时考、是否公布成绩/答案。和试卷是两张表。 */
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String examName;
    private Long paperId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer allowSubmitMinutes;
    private Integer resultVisible;
    private Integer answerVisible;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
}
