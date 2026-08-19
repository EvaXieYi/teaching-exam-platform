package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/** 登录用户表。 */
public interface SysUserMapper extends BaseMapper<SysUser> {
}
