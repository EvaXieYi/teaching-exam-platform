package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.QuestionCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 题库分类表。 */
public interface QuestionCategoryMapper extends BaseMapper<QuestionCategory> {
}
