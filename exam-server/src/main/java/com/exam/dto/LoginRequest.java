package com.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
/** 登录请求体。 */
public class LoginRequest {
    @NotBlank(message = "请输入用户名")
    private String username;
    @NotBlank(message = "请输入密码")
    private String password;
}
