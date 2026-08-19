package com.exam.dto;

import lombok.Data;

@Data
/** 新增/编辑管理员或教师账号。 */
public class UserSaveRequest {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String password;
    private Integer status;
}
