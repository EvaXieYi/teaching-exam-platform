package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_student")
/** 本场考试允许参加的学生及参考状态 NOT_STARTED/ANSWERING/SUBMITTED。 */
public class ExamStudent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long studentId;
    private String examStatus;
}
