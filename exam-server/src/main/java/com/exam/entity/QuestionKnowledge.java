package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("question_knowledge")
/** 题目与知识点多对多。一道题可对应多个知识点，学情统计靠这张表分摊分数。 */
public class QuestionKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long knowledgePointId;
    private BigDecimal weight;
}
