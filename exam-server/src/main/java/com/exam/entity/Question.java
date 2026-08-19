package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question")
/** 题目 question。questionType：SINGLE/MULTIPLE/JUDGE/FILL/ESSAY。status=0 为逻辑删除。 */
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String questionType;
    private String content;
    private String correctAnswer;
    private String analysis;
    private Integer difficulty;
    private BigDecimal defaultScore;
    private String visibility;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
