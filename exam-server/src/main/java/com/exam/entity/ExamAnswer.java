package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
/** 每题作答明细。开始考试时写入题干和正确答案快照，之后改题库不影响已考记录。 */
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
