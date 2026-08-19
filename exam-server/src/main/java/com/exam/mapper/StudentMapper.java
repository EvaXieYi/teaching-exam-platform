package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 学生档案表。 */
public interface StudentMapper extends BaseMapper<Student> {
}
