package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.KnowledgePoint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 知识点表。 */
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {
}
