package com.exam.controller;

import com.alibaba.excel.EasyExcel;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.StudentImportRow;
import com.exam.dto.StudentSaveRequest;
import com.exam.dto.UserSaveRequest;
import com.exam.entity.Student;
import com.exam.entity.SysUser;
import com.exam.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/api/users")
    public Result<PageResult<SysUser>> users(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String role) {
        return Result.ok(accountService.pageUsers(page, size, keyword, role));
    }

    @PostMapping("/api/users")
    public Result<Void> createUser(@RequestBody UserSaveRequest req) {
        req.setId(null);
        accountService.saveUser(req);
        return Result.ok();
    }

    @PutMapping("/api/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserSaveRequest req) {
        req.setId(id);
        accountService.saveUser(req);
        return Result.ok();
    }

    @GetMapping("/api/students")
    public Result<PageResult<Student>> students(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String className) {
        return Result.ok(accountService.pageStudents(page, size, keyword, className));
    }

    @GetMapping("/api/students/options")
    public Result<List<Student>> studentOptions() {
        return Result.ok(accountService.listStudents());
    }

    @PostMapping("/api/students")
    public Result<Void> createStudent(@RequestBody StudentSaveRequest req) {
        req.setId(null);
        accountService.saveStudent(req);
        return Result.ok();
    }

    @PutMapping("/api/students/{id}")
    public Result<Void> updateStudent(@PathVariable Long id, @RequestBody StudentSaveRequest req) {
        req.setId(id);
        accountService.saveStudent(req);
        return Result.ok();
    }

    @DeleteMapping("/api/students/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        accountService.deleteStudent(id);
        return Result.ok();
    }

    @PostMapping("/api/students/import")
    public Result<Integer> importStudents(@RequestParam("file") MultipartFile file) throws IOException {
        List<StudentImportRow> rows = EasyExcel.read(file.getInputStream())
                .head(StudentImportRow.class).sheet().doReadSync();
        return Result.ok(accountService.importStudents(rows));
    }
}
