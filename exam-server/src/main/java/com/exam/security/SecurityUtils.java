package com.exam.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 从 SecurityContext 取出当前登录用户；学生接口用 requireStudentId。 */
public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) auth.getPrincipal();
    }

    public static LoginUser requireUser() {
        LoginUser user = current();
        if (user == null) {
            throw new com.exam.common.BizException(401, "未登录");
        }
        return user;
    }

    public static boolean isAdmin() {
        LoginUser user = current();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static Long requireStudentId() {
        LoginUser user = requireUser();
        if (user.getStudentId() == null) {
            throw new com.exam.common.BizException(403, "当前账号不是考生");
        }
        return user.getStudentId();
    }
}
