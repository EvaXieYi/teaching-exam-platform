package com.exam.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class LoginUser implements UserDetails {
    private final Long userId;
    private final Long studentId;
    private final String username;
    private final String password;
    private final String realName;
    private final String role;
    private final boolean enabled;

    public LoginUser(Long userId, Long studentId, String username, String password,
                     String realName, String role, boolean enabled) {
        this.userId = userId;
        this.studentId = studentId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
