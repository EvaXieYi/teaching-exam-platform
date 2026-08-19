package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_knowledge_stat")
/** 学生知识点掌握汇总，阅卷完成后由 KnowledgeStatService 回写。 */
public class StudentKnowledgeStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long knowledgePointId;
    private Integer examCount;
    private Integer questionCount;
    private Integer correctCount;
    private BigDecimal gotScore;
    private BigDecimal fullScore;
    private BigDecimal masteryRate;
    private Long lastExamId;
    private LocalDateTime updatedAt;
}
