package com.exam.dto;

import lombok.Data;

@Data
/** 登录成功返回：token + 角色，前端用来跳转教师端或学生端。 */
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long studentId;
}
