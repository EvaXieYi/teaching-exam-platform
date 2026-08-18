package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.AnswerSaveRequest;
import com.exam.service.AnalysisService;
import com.exam.service.StudentExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentExamController {
    private final StudentExamService studentExamService;
    private final AnalysisService analysisService;

    @GetMapping("/exams")
    public Result<List<StudentExamService.StudentExamVO>> exams() {
        return Result.ok(studentExamService.myExams());
    }

    @PostMapping("/exams/{id}/start")
    public Result<StudentExamService.TakeVO> start(@PathVariable Long id) {
        return Result.ok(studentExamService.start(id));
    }

    @GetMapping("/records/{recordId}/questions")
    public Result<StudentExamService.TakeVO> questions(@PathVariable Long recordId) {
        return Result.ok(studentExamService.questions(recordId));
    }

    @PutMapping("/records/{recordId}/answers")
    public Result<Void> save(@PathVariable Long recordId, @RequestBody List<AnswerSaveRequest> answers) {
        studentExamService.saveAnswers(recordId, answers);
        return Result.ok();
    }

    @PostMapping("/records/{recordId}/submit")
    public Result<Void> submit(@PathVariable Long recordId, @RequestBody(required = false) List<AnswerSaveRequest> answers) {
        studentExamService.submit(recordId, answers);
        return Result.ok();
    }

    @GetMapping("/records/{recordId}/review")
    public Result<StudentExamService.ReviewVO> review(@PathVariable Long recordId) {
        return Result.ok(studentExamService.review(recordId));
    }

    @GetMapping("/knowledge-stats")
    public Result<List<AnalysisService.KnowledgeRow>> knowledge() {
        return Result.ok(analysisService.studentKnowledge(null));
    }
}
