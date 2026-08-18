package com.exam.dto;

import lombok.Data;

@Data
public class UserSaveRequest {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String password;
    private Integer status;
}
