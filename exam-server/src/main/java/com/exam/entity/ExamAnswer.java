package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long questionId;
    private String studentAnswer;
    private String correctAnswerSnapshot;
    private String questionContentSnapshot;
    private String questionTypeSnapshot;
    private BigDecimal questionScore;
    private BigDecimal score;
    private Integer isCorrect;
    private Integer flagged;
    private String comment;
    private Long markedBy;
    private LocalDateTime markedAt;
    private LocalDateTime updatedAt;
}
