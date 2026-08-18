package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("question_knowledge")
public class QuestionKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long knowledgePointId;
    private BigDecimal weight;
}
