package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BizException;
import com.exam.dto.MarkingRequest;
import com.exam.entity.Exam;
import com.exam.entity.ExamAnswer;
import com.exam.entity.ExamRecord;
import com.exam.entity.Student;
import com.exam.mapper.ExamAnswerMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.StudentMapper;
import com.exam.security.SecurityUtils;
import com.exam.util.QuestionTypes;
import com.exam.util.ScoreCalculator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/** 主观题（简答/关键词解释）人工阅卷。该份答卷全部主观题批完后汇总主观分、出总分，并回写知识点掌握度。 */
public class MarkingService {
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final ExamMapper examMapper;
    private final StudentMapper studentMapper;
    private final StudentExamService studentExamService;
    private final OperLogService operLogService;

    public List<MarkingRecordVO> list(Long examId, String status) {
        LambdaQueryWrapper<ExamRecord> w = new LambdaQueryWrapper<>();
        if (examId != null) {
            w.eq(ExamRecord::getExamId, examId);
        }
        if (status != null && !status.isEmpty()) {
            w.eq(ExamRecord::getRecordStatus, status);
        } else {
            w.in(ExamRecord::getRecordStatus, "MARKING", "FINISHED");
        }
        w.orderByDesc(ExamRecord::getSubmitTime);
        List<ExamRecord> records = recordMapper.selectList(w);
        List<MarkingRecordVO> list = new ArrayList<>();
        for (ExamRecord record : records) {
            MarkingRecordVO vo = new MarkingRecordVO();
            vo.setRecord(record);
            Exam exam = examMapper.selectById(record.getExamId());
            if (exam != null) {
                vo.setExamName(exam.getExamName());
            }
            Student student = studentMapper.selectById(record.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getName());
                vo.setStudentNo(student.getStudentNo());
                vo.setClassName(student.getClassName());
            }
            long pending = answerMapper.selectCount(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId())
                    .in(ExamAnswer::getQuestionTypeSnapshot, QuestionTypes.ESSAY, QuestionTypes.TERM)
                    .isNull(ExamAnswer::getMarkedAt));
            vo.setPendingEssay(pending);
            list.add(vo);
        }
        return list;
    }

    public MarkingDetailVO detail(Long recordId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException("答卷不存在");
        }
        MarkingDetailVO vo = new MarkingDetailVO();
        vo.setRecord(record);
        Exam exam = examMapper.selectById(record.getExamId());
        vo.setExamName(exam.getExamName());
        Student student = studentMapper.selectById(record.getStudentId());
        vo.setStudentName(student.getName());
        vo.setStudentNo(student.getStudentNo());
        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId).orderByAsc(ExamAnswer::getId));
        vo.setAnswers(answers);
        return vo;
    }

    @Transactional
    public void mark(Long answerId, MarkingRequest req) {
        ExamAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) {
            throw new BizException("答题记录不存在");
        }
        if (!ScoreCalculator.isSubjective(answer.getQuestionTypeSnapshot())) {
            throw new BizException("客观题无需人工阅卷");
        }
        if (req.getScore() == null || req.getScore().compareTo(BigDecimal.ZERO) < 0
                || req.getScore().compareTo(answer.getQuestionScore()) > 0) {
            throw new BizException("得分必须在 0 到本题满分之间");
        }
        answer.setScore(req.getScore());
        answer.setComment(req.getComment());
        answer.setMarkedBy(SecurityUtils.requireUser().getUserId());
        answer.setMarkedAt(LocalDateTime.now());
        if (answer.getQuestionScore().compareTo(BigDecimal.ZERO) == 0) {
            answer.setIsCorrect(1);
        } else {
            answer.setIsCorrect(req.getScore().compareTo(answer.getQuestionScore()) >= 0 ? 1 : 0);
        }
        answerMapper.updateById(answer);
        ExamRecord record = recordMapper.selectById(answer.getRecordId());
        long pending = answerMapper.selectCount(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId())
                .in(ExamAnswer::getQuestionTypeSnapshot, QuestionTypes.ESSAY, QuestionTypes.TERM)
                .isNull(ExamAnswer::getMarkedAt));
        if (pending == 0) {
            List<ExamAnswer> essays = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId())
                    .in(ExamAnswer::getQuestionTypeSnapshot, QuestionTypes.ESSAY, QuestionTypes.TERM));
            BigDecimal subjective = essays.stream()
                    .map(a -> a.getScore() == null ? BigDecimal.ZERO : a.getScore())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Exam exam = examMapper.selectById(record.getExamId());
            studentExamService.finishRecord(record, exam, subjective);
            operLogService.log("完成阅卷", "recordId=" + record.getId());
        }
    }

    @Data
    public static class MarkingRecordVO {
        private ExamRecord record;
        private String examName;
        private String studentName;
        private String studentNo;
        private String className;
        private Long pendingEssay;
    }

    @Data
    public static class MarkingDetailVO {
        private ExamRecord record;
        private String examName;
        private String studentName;
        private String studentNo;
        private List<ExamAnswer> answers;
    }
}
