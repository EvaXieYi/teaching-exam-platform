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
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamPaperMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ExamStudentMapper;
import com.exam.mapper.StudentMapper;
import com.exam.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamManageService {
    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamRecordMapper recordMapper;
    private final StudentMapper studentMapper;
    private final OperLogService operLogService;

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
        data.put("studentCount", studentMapper.selectCount(null));
        data.put("examCount", examMapper.selectCount(null));
        long pendingMark = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getRecordStatus, "MARKING"));
        data.put("pendingMarking", pendingMark);
        List<Exam> recent = examMapper.selectList(new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getId).last("LIMIT 5"));
        data.put("recentExams", recent.stream().map(this::toVO).collect(Collectors.toList()));
        data.put("classNames", studentMapper.selectList(null).stream()
                .map(Student::getClassName).filter(StringUtils::hasText).distinct().collect(Collectors.toList()));
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
