package com.exam.dto;

import lombok.Data;

@Data
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
