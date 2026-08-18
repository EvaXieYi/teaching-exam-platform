package com.exam.controller;

import com.alibaba.excel.EasyExcel;
import com.exam.common.Result;
import com.exam.dto.MarkingRequest;
import com.exam.entity.ExamRecord;
import com.exam.entity.Student;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.StudentMapper;
import com.exam.service.AnalysisService;
import com.exam.service.MarkingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MarkingAnalysisController {
    private final MarkingService markingService;
    private final AnalysisService analysisService;
    private final StudentMapper studentMapper;
    private final ExamMapper examMapper;

    @GetMapping("/api/marking/records")
    public Result<List<MarkingService.MarkingRecordVO>> markingList(@RequestParam(required = false) Long examId,
                                                                    @RequestParam(required = false) String status) {
        return Result.ok(markingService.list(examId, status));
    }

    @GetMapping("/api/marking/records/{recordId}")
    public Result<MarkingService.MarkingDetailVO> markingDetail(@PathVariable Long recordId) {
        return Result.ok(markingService.detail(recordId));
    }

    @PostMapping("/api/marking/answers/{answerId}")
    public Result<Void> mark(@PathVariable Long answerId, @RequestBody MarkingRequest req) {
        markingService.mark(answerId, req);
        return Result.ok();
    }

    @GetMapping("/api/exams/{id}/statistics")
    public Result<Map<String, Object>> statistics(@PathVariable Long id) {
        return Result.ok(analysisService.examStatistics(id));
    }

    @GetMapping("/api/exams/{id}/knowledge-stats")
    public Result<Object> examKnowledge(@PathVariable Long id) {
        return Result.ok(analysisService.examStatistics(id).get("knowledge"));
    }

    @GetMapping("/api/analysis/students/{studentId}/knowledge")
    public Result<List<AnalysisService.KnowledgeRow>> studentKnowledge(@PathVariable Long studentId) {
        return Result.ok(analysisService.studentKnowledge(studentId));
    }

    @GetMapping("/api/analysis/classes/{className}/knowledge")
    public Result<List<AnalysisService.KnowledgeRow>> classKnowledge(@PathVariable String className) {
        return Result.ok(analysisService.classKnowledge(className));
    }

    @GetMapping("/api/scores")
    public Result<List<ScoreRow>> scores(@RequestParam(required = false) Long examId,
                                         @RequestParam(required = false) String className) {
        return Result.ok(toRows(analysisService.scoreList(examId, className)));
    }

    @GetMapping("/api/scores/export")
    public void export(@RequestParam(required = false) Long examId,
                       @RequestParam(required = false) String className,
                       HttpServletResponse response) throws IOException {
        List<ScoreRow> rows = toRows(analysisService.scoreList(examId, className));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String name = URLEncoder.encode("成绩导出.xlsx", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + name);
        EasyExcel.write(response.getOutputStream(), ScoreRow.class).sheet("成绩").doWrite(rows);
    }

    private List<ScoreRow> toRows(List<ExamRecord> records) {
        List<ScoreRow> rows = new ArrayList<>();
        for (ExamRecord r : records) {
            ScoreRow row = new ScoreRow();
            Student s = studentMapper.selectById(r.getStudentId());
            if (s != null) {
                row.setStudentNo(s.getStudentNo());
                row.setStudentName(s.getName());
                row.setClassName(s.getClassName());
            }
            if (examMapper.selectById(r.getExamId()) != null) {
                row.setExamName(examMapper.selectById(r.getExamId()).getExamName());
            }
            row.setObjectiveScore(r.getObjectiveScore() == null ? null : r.getObjectiveScore().toPlainString());
            row.setSubjectiveScore(r.getSubjectiveScore() == null ? null : r.getSubjectiveScore().toPlainString());
            row.setTotalScore(r.getTotalScore() == null ? null : r.getTotalScore().toPlainString());
            row.setPassed(Integer.valueOf(1).equals(r.getPassed()) ? "及格" : "不及格");
            rows.add(row);
        }
        return rows;
    }

    @Data
    public static class ScoreRow {
        @com.alibaba.excel.annotation.ExcelProperty("考试")
        private String examName;
        @com.alibaba.excel.annotation.ExcelProperty("学号")
        private String studentNo;
        @com.alibaba.excel.annotation.ExcelProperty("姓名")
        private String studentName;
        @com.alibaba.excel.annotation.ExcelProperty("班级")
        private String className;
        @com.alibaba.excel.annotation.ExcelProperty("客观题")
        private String objectiveScore;
        @com.alibaba.excel.annotation.ExcelProperty("主观题")
        private String subjectiveScore;
        @com.alibaba.excel.annotation.ExcelProperty("总分")
        private String totalScore;
        @com.alibaba.excel.annotation.ExcelProperty("是否及格")
        private String passed;
    }
}
