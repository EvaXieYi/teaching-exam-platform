package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.common.PageResult;
import com.exam.dto.StudentImportRow;
import com.exam.dto.StudentSaveRequest;
import com.exam.dto.UserSaveRequest;
import com.exam.entity.Student;
import com.exam.entity.SysUser;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<SysUser> pageUsers(long page, long size, String keyword, String role) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        if (StringUtils.hasText(role)) {
            w.eq(SysUser::getRole, role);
        }
        w.orderByDesc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), w);
        p.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    @Transactional
    public void saveUser(UserSaveRequest req) {
        if (!"ADMIN".equals(req.getRole()) && !"TEACHER".equals(req.getRole())) {
            throw new BizException("只能创建管理员或教师账号");
        }
        SysUser user = req.getId() == null ? new SysUser() : userMapper.selectById(req.getId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (req.getId() == null) {
            if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername())) > 0) {
                throw new BizException("用户名已存在");
            }
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUsername(req.getUsername());
        user.setRealName(req.getRealName());
        user.setRole(req.getRole());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        } else if (req.getId() == null) {
            user.setPassword(passwordEncoder.encode("123456"));
        }
        user.setUpdatedAt(LocalDateTime.now());
        if (req.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
    }

    public PageResult<Student> pageStudents(long page, long size, String keyword, String className) {
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(Student::getName, keyword).or().like(Student::getStudentNo, keyword));
        }
        if (StringUtils.hasText(className)) {
            w.eq(Student::getClassName, className);
        }
        w.orderByDesc(Student::getId);
        Page<Student> p = studentMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public List<Student> listStudents() {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 1).orderByAsc(Student::getStudentNo));
    }

    @Transactional
    public void saveStudent(StudentSaveRequest req) {
        LocalDateTime now = LocalDateTime.now();
        if (req.getId() == null) {
            if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, req.getStudentNo())) > 0) {
                throw new BizException("学号已存在");
            }
            if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getStudentNo())) > 0) {
                throw new BizException("学号已被用作登录名");
            }
            SysUser user = new SysUser();
            user.setUsername(req.getStudentNo());
            user.setPassword(passwordEncoder.encode(StringUtils.hasText(req.getPassword()) ? req.getPassword() : "student123"));
            user.setRealName(req.getName());
            user.setRole("STUDENT");
            user.setStatus(1);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);
            Student student = new Student();
            fillStudent(student, req);
            student.setUserId(user.getId());
            student.setStatus(1);
            student.setCreatedAt(now);
            student.setUpdatedAt(now);
            studentMapper.insert(student);
        } else {
            Student student = studentMapper.selectById(req.getId());
            if (student == null) {
                throw new BizException("学生不存在");
            }
            fillStudent(student, req);
            student.setUpdatedAt(now);
            studentMapper.updateById(student);
            SysUser user = userMapper.selectById(student.getUserId());
            if (user != null) {
                user.setRealName(req.getName());
                if (StringUtils.hasText(req.getPassword())) {
                    user.setPassword(passwordEncoder.encode(req.getPassword()));
                }
                user.setUpdatedAt(now);
                userMapper.updateById(user);
            }
        }
    }

    @Transactional
    public int importStudents(List<StudentImportRow> rows) {
        int count = 0;
        for (StudentImportRow row : rows) {
            if (!StringUtils.hasText(row.getStudentNo()) || !StringUtils.hasText(row.getName())) {
                continue;
            }
            if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, row.getStudentNo())) > 0) {
                continue;
            }
            StudentSaveRequest req = new StudentSaveRequest();
            req.setStudentNo(row.getStudentNo().trim());
            req.setName(row.getName().trim());
            req.setDepartment(row.getDepartment());
            req.setClassName(row.getClassName());
            req.setPhone(row.getPhone());
            req.setEmail(row.getEmail());
            saveStudent(req);
            count++;
        }
        return count;
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            return;
        }
        studentMapper.deleteById(id);
        userMapper.deleteById(student.getUserId());
    }

    private void fillStudent(Student student, StudentSaveRequest req) {
        student.setStudentNo(req.getStudentNo());
        student.setName(req.getName());
        student.setDepartment(req.getDepartment());
        student.setClassName(req.getClassName());
        student.setPhone(req.getPhone());
        student.setEmail(req.getEmail());
    }
}
