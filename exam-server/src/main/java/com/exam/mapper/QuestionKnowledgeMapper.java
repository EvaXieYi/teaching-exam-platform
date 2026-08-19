package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.QuestionKnowledge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 题目-知识点关联表。 */
public interface QuestionKnowledgeMapper extends BaseMapper<QuestionKnowledge> {
}
