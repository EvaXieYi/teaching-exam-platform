package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.LoginRequest;
import com.exam.dto.LoginVO;
import com.exam.security.LoginUser;
import com.exam.security.SecurityUtils;
import com.exam.service.AuthService;
import com.exam.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/** 登录、查当前用户、退出。JWT 无状态，logout 主要由前端删掉 token。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AvatarService avatarService;

    /** 校验用户名密码，返回 token 和角色，前端按角色跳教师端或学生端。 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<LoginVO> me() {
        LoginUser user = SecurityUtils.requireUser();
        return Result.ok(authService.profile(user));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar() {
        return avatarService.image(SecurityUtils.requireUser().getUserId());
    }

    @PostMapping("/avatar")
    public Result<Void> uploadAvatar(@RequestParam("file") MultipartFile file) {
        avatarService.save(SecurityUtils.requireUser().getUserId(), file);
        return Result.ok();
    }

    @DeleteMapping("/avatar")
    public Result<Void> deleteAvatar() {
        avatarService.delete(SecurityUtils.requireUser().getUserId());
        return Result.ok();
    }
}
