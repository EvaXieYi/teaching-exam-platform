package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 题目表。 */
public interface QuestionMapper extends BaseMapper<Question> {
}
