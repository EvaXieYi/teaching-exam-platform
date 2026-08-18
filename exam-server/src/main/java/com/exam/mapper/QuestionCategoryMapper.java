package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.QuestionCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionCategoryMapper extends BaseMapper<QuestionCategory> {
}
