package com.exam.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.Student;
import com.exam.entity.SysUser;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        Long studentId = null;
        if ("STUDENT".equals(user.getRole())) {
            Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, user.getId()));
            if (student != null) {
                studentId = student.getId();
            }
        }
        return new LoginUser(user.getId(), studentId, user.getUsername(), user.getPassword(),
                user.getRealName(), user.getRole(), user.getStatus() != null && user.getStatus() == 1);
    }
}
