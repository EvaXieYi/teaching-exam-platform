package com.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.SysOperLog;
import com.exam.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 发布考试、完成阅卷等操作日志，供管理员查看。 */
@RestController
@RequiredArgsConstructor
public class OperLogController {
    private final SysOperLogMapper logMapper;

    @GetMapping("/api/logs")
    public Result<PageResult<SysOperLog>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        Page<SysOperLog> p = logMapper.selectPage(new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOperLog>()
                        .orderByDesc(SysOperLog::getId));
        return Result.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }
}
