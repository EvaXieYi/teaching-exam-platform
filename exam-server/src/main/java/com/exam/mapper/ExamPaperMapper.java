package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.ExamPaper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 试卷表。 */
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {
}
