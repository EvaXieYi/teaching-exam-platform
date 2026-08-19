package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.ExamSaveRequest;
import com.exam.service.ExamManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 考试任务：时间窗、指定考生、发布/停止；工作台统计也在这里。 */
@RestController
@RequiredArgsConstructor
public class ExamManageController {
    private final ExamManageService examManageService;

    @GetMapping("/api/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(examManageService.dashboard());
    }

    @GetMapping("/api/exams")
    public Result<PageResult<ExamManageService.ExamVO>> page(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status) {
        return Result.ok(examManageService.page(page, size, keyword, status));
    }

    @GetMapping("/api/exams/{id}")
    public Result<ExamManageService.ExamVO> detail(@PathVariable Long id) {
        return Result.ok(examManageService.detail(id));
    }

    @PostMapping("/api/exams")
    public Result<Long> create(@RequestBody ExamSaveRequest req) {
        req.setId(null);
        return Result.ok(examManageService.save(req));
    }

    @PutMapping("/api/exams/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody ExamSaveRequest req) {
        req.setId(id);
        return Result.ok(examManageService.save(req));
    }

    @PostMapping("/api/exams/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        examManageService.publish(id);
        return Result.ok();
    }

    @PostMapping("/api/exams/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        examManageService.stop(id);
        return Result.ok();
    }
}
