package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.PaperSaveRequest;
import com.exam.entity.ExamPaper;
import com.exam.service.PaperService;
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

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService paperService;

    @GetMapping
    public Result<PageResult<ExamPaper>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(paperService.page(page, size, keyword));
    }

    @GetMapping("/options")
    public Result<List<ExamPaper>> options() {
        return Result.ok(paperService.options());
    }

    @GetMapping("/{id}")
    public Result<PaperService.PaperVO> detail(@PathVariable Long id) {
        return Result.ok(paperService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody PaperSaveRequest req) {
        req.setId(null);
        return Result.ok(paperService.save(req));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody PaperSaveRequest req) {
        req.setId(id);
        return Result.ok(paperService.save(req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return Result.ok();
    }
}
