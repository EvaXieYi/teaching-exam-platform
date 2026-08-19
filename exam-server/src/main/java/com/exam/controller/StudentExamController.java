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

/**
 * 学生端接口，仅 ROLE_STUDENT 可访问。
 * 开始考试 → 拉题（不含答案）→ 自动保存 → 交卷 → 查看成绩/知识点掌握。
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentExamController {
    private final StudentExamService studentExamService;
    private final AnalysisService analysisService;

    /** 当前学生待考/已考列表。 */
    @GetMapping("/exams")
    public Result<List<StudentExamService.StudentExamVO>> exams() {
        return Result.ok(studentExamService.myExams());
    }

    /** 开始或继续考试：后端记录开始时间，倒计时以服务端为准。 */
    @PostMapping("/exams/{id}/start")
    public Result<StudentExamService.TakeVO> start(@PathVariable Long id) {
        return Result.ok(studentExamService.start(id));
    }

    /** 拉试卷题目，不返回正确答案和解析。 */
    @GetMapping("/records/{recordId}/questions")
    public Result<StudentExamService.TakeVO> questions(@PathVariable Long recordId) {
        return Result.ok(studentExamService.questions(recordId));
    }

    /** 自动保存答案（切题或定时调用）。 */
    @PutMapping("/records/{recordId}/answers")
    public Result<Void> save(@PathVariable Long recordId, @RequestBody List<AnswerSaveRequest> answers) {
        studentExamService.saveAnswers(recordId, answers);
        return Result.ok();
    }

    /** 交卷：客观题立即评分；有简答题则进入待阅卷。 */
    @PostMapping("/records/{recordId}/submit")
    public Result<Void> submit(@PathVariable Long recordId, @RequestBody(required = false) List<AnswerSaveRequest> answers) {
        studentExamService.submit(recordId, answers);
        return Result.ok();
    }

    /** 交卷后查看作答；是否显示标准答案由该场考试的 answerVisible 决定。 */
    @GetMapping("/records/{recordId}/review")
    public Result<StudentExamService.ReviewVO> review(@PathVariable Long recordId) {
        return Result.ok(studentExamService.review(recordId));
    }

    /** 当前学生各知识点掌握度（阅卷完成后才有数据）。 */
    @GetMapping("/knowledge-stats")
    public Result<List<AnalysisService.KnowledgeRow>> knowledge() {
        return Result.ok(analysisService.studentKnowledge(null));
    }
}
