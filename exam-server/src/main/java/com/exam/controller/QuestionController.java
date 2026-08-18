package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.QuestionSaveRequest;
import com.exam.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

    @GetMapping
    public Result<PageResult<QuestionService.QuestionVO>> page(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               @RequestParam(required = false) String type,
                                                               @RequestParam(required = false) Long categoryId,
                                                               @RequestParam(required = false) Long knowledgePointId,
                                                               @RequestParam(required = false) String keyword) {
        return Result.ok(questionService.page(page, size, type, categoryId, knowledgePointId, keyword));
    }

    @GetMapping("/{id}")
    public Result<QuestionService.QuestionVO> detail(@PathVariable Long id) {
        return Result.ok(questionService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody QuestionSaveRequest req) {
        req.setId(null);
        return Result.ok(questionService.save(req));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody QuestionSaveRequest req) {
        req.setId(id);
        return Result.ok(questionService.save(req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
