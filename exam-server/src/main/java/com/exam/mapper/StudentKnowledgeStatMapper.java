package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.StudentKnowledgeStat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 学生知识点掌握汇总表。 */
public interface StudentKnowledgeStatMapper extends BaseMapper<StudentKnowledgeStat> {
}
