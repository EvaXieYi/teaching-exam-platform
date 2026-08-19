package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 操作日志表。 */
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
