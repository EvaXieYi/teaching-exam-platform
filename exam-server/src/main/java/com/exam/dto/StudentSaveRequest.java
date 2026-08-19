package com.exam.dto;

import lombok.Data;

@Data
/** 新增/编辑学生。学号同时作为登录名。 */
public class StudentSaveRequest {
    private Long id;
    private String studentNo;
    private String name;
    private String department;
    private String className;
    private String phone;
    private String email;
    private String password;
}
