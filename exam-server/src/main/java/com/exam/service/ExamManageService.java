package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.common.PageResult;
import com.exam.dto.ExamSaveRequest;
import com.exam.entity.Exam;
import com.exam.entity.ExamPaper;
import com.exam.entity.ExamRecord;
import com.exam.entity.ExamStudent;
import com.exam.entity.Student;
import com.exam.entity.SysOperLog;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamPaperMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ExamStudentMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.SysOperLogMapper;
import com.exam.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/** 考试任务：草稿可改，发布后指定考生可见；runtimeStatus 按当前时间计算未开始/进行中/已结束。 */
public class ExamManageService {
    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamRecordMapper recordMapper;
    private final StudentMapper studentMapper;
    private final OperLogService operLogService;
    private final SysOperLogMapper logMapper;

    public PageResult<ExamVO> page(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Exam> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(Exam::getExamName, keyword);
        }
        if (StringUtils.hasText(status)) {
            w.eq(Exam::getStatus, status);
        }
        if (!SecurityUtils.isAdmin()) {
            w.eq(Exam::getCreatedBy, SecurityUtils.requireUser().getUserId());
        }
        w.orderByDesc(Exam::getId);
        Page<Exam> p = examMapper.selectPage(new Page<>(page, size), w);
        List<ExamVO> records = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), records);
    }

    public ExamVO detail(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BizException("考试不存在");
        }
        ExamVO vo = toVO(exam);
        List<ExamStudent> list = examStudentMapper.selectList(
                new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, id));
        vo.setStudentIds(list.stream().map(ExamStudent::getStudentId).collect(Collectors.toList()));
        return vo;
    }

    @Transactional
    public Long save(ExamSaveRequest req) {
        if (req.getStartTime() == null || req.getEndTime() == null || !req.getEndTime().isAfter(req.getStartTime())) {
            throw new BizException("请设置正确的考试时间");
        }
        ExamPaper paper = paperMapper.selectById(req.getPaperId());
        if (paper == null || paper.getStatus() == 0) {
            throw new BizException("试卷不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        Exam exam = req.getId() == null ? new Exam() : examMapper.selectById(req.getId());
        if (exam == null) {
            throw new BizException("考试不存在");
        }
        if (req.getId() != null && !"DRAFT".equals(exam.getStatus())) {
            throw new BizException("已发布的考试不能直接修改，请停止后再调整或复制新考试");
        }
        exam.setExamName(req.getExamName());
        exam.setPaperId(req.getPaperId());
        exam.setStartTime(req.getStartTime());
        exam.setEndTime(req.getEndTime());
        exam.setDurationMinutes(req.getDurationMinutes());
        exam.setAllowSubmitMinutes(req.getAllowSubmitMinutes() == null ? 0 : req.getAllowSubmitMinutes());
        exam.setResultVisible(req.getResultVisible() == null ? 1 : req.getResultVisible());
        exam.setAnswerVisible(req.getAnswerVisible() == null ? 0 : req.getAnswerVisible());
        if (req.getId() == null) {
            exam.setStatus("DRAFT");
            exam.setCreatedBy(SecurityUtils.requireUser().getUserId());
            exam.setCreatedAt(now);
            examMapper.insert(exam);
        } else {
            examMapper.updateById(exam);
            examStudentMapper.delete(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, exam.getId()));
        }
        replaceStudents(exam.getId(), req.getStudentIds());
        return exam.getId();
    }

    @Transactional
    public void publish(Long id) {
        Exam exam = requireExam(id);
        Long count = examStudentMapper.selectCount(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, id));
        if (count == 0) {
            throw new BizException("请先指定考生");
        }
        exam.setStatus("PUBLISHED");
        examMapper.updateById(exam);
        operLogService.log("发布考试", exam.getExamName());
    }

    @Transactional
    public void stop(Long id) {
        Exam exam = requireExam(id);
        exam.setStatus("FINISHED");
        exam.setEndTime(LocalDateTime.now());
        examMapper.updateById(exam);
        operLogService.log("停止考试", exam.getExamName());
    }

    private void replaceStudents(Long examId, List<Long> studentIds) {
        if (studentIds == null) {
            return;
        }
        for (Long sid : studentIds) {
            ExamStudent row = new ExamStudent();
            row.setExamId(examId);
            row.setStudentId(sid);
            row.setExamStatus("NOT_STARTED");
            examStudentMapper.insert(row);
        }
    }

    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BizException("考试不存在");
        }
        return exam;
    }

    private ExamVO toVO(Exam exam) {
        ExamVO vo = new ExamVO();
        vo.setExam(exam);
        vo.setRuntimeStatus(runtimeStatus(exam));
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        if (paper != null) {
            vo.setPaperName(paper.getPaperName());
            vo.setTotalScore(paper.getTotalScore());
            vo.setPassScore(paper.getPassScore());
        }
        vo.setStudentCount(examStudentMapper.selectCount(
                new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, exam.getId())));
        vo.setSubmittedCount(recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, exam.getId())
                .in(ExamRecord::getRecordStatus, "SUBMITTED", "MARKING", "FINISHED")));
        return vo;
    }

    public static String runtimeStatus(Exam exam) {
        if ("DRAFT".equals(exam.getStatus())) {
            return "DRAFT";
        }
        if ("FINISHED".equals(exam.getStatus())) {
            return "FINISHED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            return "PUBLISHED";
        }
        if (now.isAfter(exam.getEndTime())) {
            return "FINISHED";
        }
        return "ONGOING";
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> data = new HashMap<>();
        List<Student> students = studentMapper.selectList(null);
        data.put("studentCount", (long) students.size());

        List<String> classNames = students.stream()
                .map(Student::getClassName)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        data.put("classNames", classNames);
        data.put("classCount", (long) classNames.size());

        List<Map<String, Object>> classStats = students.stream()
                .filter(s -> StringUtils.hasText(s.getClassName()))
                .collect(Collectors.groupingBy(Student::getClassName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("className", e.getKey());
                    row.put("studentCount", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
        data.put("classStats", classStats);

        LambdaQueryWrapper<Exam> examW = new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getId);
        if (!SecurityUtils.isAdmin()) {
            examW.eq(Exam::getCreatedBy, SecurityUtils.requireUser().getUserId());
        }
        List<Exam> exams = examMapper.selectList(examW);
        data.put("examCount", (long) exams.size());
        data.put("ongoingExamCount", exams.stream().filter(e -> "ONGOING".equals(runtimeStatus(e))).count());

        List<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toList());
        long pendingMark = 0;
        long answering = 0;
        long submitted = 0;
        BigDecimal avgScore = BigDecimal.ZERO;
        BigDecimal passRate = BigDecimal.ZERO;
        if (!examIds.isEmpty()) {
            List<ExamRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ExamRecord>().in(ExamRecord::getExamId, examIds));
            pendingMark = records.stream().filter(r -> "MARKING".equals(r.getRecordStatus())).count();
            answering = records.stream().filter(r -> "ANSWERING".equals(r.getRecordStatus())).count();
            submitted = records.stream().filter(r -> !"ANSWERING".equals(r.getRecordStatus())).count();
            List<ExamRecord> finished = records.stream()
                    .filter(r -> "FINISHED".equals(r.getRecordStatus()))
                    .collect(Collectors.toList());
            if (!finished.isEmpty()) {
                BigDecimal sum = finished.stream()
                        .map(r -> r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                avgScore = sum.divide(BigDecimal.valueOf(finished.size()), 1, RoundingMode.HALF_UP);
                long passed = finished.stream().filter(r -> Integer.valueOf(1).equals(r.getPassed())).count();
                passRate = BigDecimal.valueOf(passed * 100.0 / finished.size()).setScale(1, RoundingMode.HALF_UP);
            }
        }
        data.put("pendingMarking", pendingMark);
        data.put("answeringCount", answering);
        data.put("submittedCount", submitted);
        data.put("avgScore", avgScore);
        data.put("passRate", passRate);
        data.put("recentExams", exams.stream().limit(5).map(this::toVO).collect(Collectors.toList()));

        List<SysOperLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<SysOperLog>().orderByDesc(SysOperLog::getId).last("LIMIT 8"));
        data.put("recentLogs", logs);
        return data;
    }

    @Data
    public static class ExamVO {
        private Exam exam;
        private String runtimeStatus;
        private String paperName;
        private java.math.BigDecimal totalScore;
        private java.math.BigDecimal passScore;
        private Long studentCount;
        private Long submittedCount;
        private List<Long> studentIds = new ArrayList<>();
    }
}
