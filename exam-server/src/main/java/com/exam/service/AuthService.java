package com.exam.service;

import com.exam.common.BizException;
import com.exam.dto.LoginRequest;
import com.exam.dto.LoginVO;
import com.exam.security.JwtUtil;
import com.exam.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        LoginUser user = (LoginUser) auth.getPrincipal();
        if (!user.isEnabled()) {
            throw new BizException("账号已停用");
        }
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.createToken(user.getUserId(), user.getUsername(), user.getRole()));
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStudentId(user.getStudentId());
        return vo;
    }

    public LoginVO profile(LoginUser user) {
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStudentId(user.getStudentId());
        return vo;
    }
}
