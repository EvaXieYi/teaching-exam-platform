package com.exam.service;

import com.exam.entity.SysOperLog;
import com.exam.mapper.SysOperLogMapper;
import com.exam.security.LoginUser;
import com.exam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperLogService {
    private final SysOperLogMapper logMapper;

    public void log(String operation, String detail) {
        LoginUser user = SecurityUtils.current();
        SysOperLog row = new SysOperLog();
        if (user != null) {
            row.setUserId(user.getUserId());
            row.setUsername(user.getUsername());
        }
        row.setOperation(operation);
        row.setDetail(detail);
        row.setCreatedAt(LocalDateTime.now());
        logMapper.insert(row);
    }
}
