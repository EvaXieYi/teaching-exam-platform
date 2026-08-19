package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 答卷成绩表。 */
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
}
