package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.QuestionOption;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 题目选项表。 */
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {
}
