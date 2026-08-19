package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.common.BizException;
import com.exam.dto.AnswerSaveRequest;
import com.exam.entity.Exam;
import com.exam.entity.ExamAnswer;
import com.exam.entity.ExamPaper;
import com.exam.entity.ExamPaperQuestion;
import com.exam.entity.ExamRecord;
import com.exam.entity.ExamStudent;
import com.exam.entity.Question;
import com.exam.entity.QuestionOption;
import com.exam.mapper.ExamAnswerMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamPaperMapper;
import com.exam.mapper.ExamPaperQuestionMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ExamStudentMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionOptionMapper;
import com.exam.security.SecurityUtils;
import com.exam.util.ScoreCalculator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 学生考试主流程：开始（冻结题目快照）→ 保存答案 → 交卷评分。
 * 拉题接口不带正确答案；剩余时间用「开始时间 + 时长」与考试截止时间的较小值。
 */
public class StudentExamService {
    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final KnowledgeStatService knowledgeStatService;

    public List<StudentExamVO> myExams() {
        Long studentId = SecurityUtils.requireStudentId();
        List<ExamStudent> mine = examStudentMapper.selectList(
                new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getStudentId, studentId));
        List<StudentExamVO> list = new ArrayList<>();
        for (ExamStudent es : mine) {
            Exam exam = examMapper.selectById(es.getExamId());
            if (exam == null || "DRAFT".equals(exam.getStatus())) {
                continue;
            }
            StudentExamVO vo = new StudentExamVO();
            vo.setExamId(exam.getId());
            vo.setExamName(exam.getExamName());
            vo.setStartTime(exam.getStartTime());
            vo.setEndTime(exam.getEndTime());
            vo.setDurationMinutes(exam.getDurationMinutes());
            vo.setRuntimeStatus(ExamManageService.runtimeStatus(exam));
            vo.setExamStatus(es.getExamStatus());
            ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, exam.getId()).eq(ExamRecord::getStudentId, studentId));
            if (record != null) {
                vo.setRecordId(record.getId());
                vo.setRecordStatus(record.getRecordStatus());
                vo.setRemainingSeconds(remainingSeconds(exam, record));
                if ("FINISHED".equals(record.getRecordStatus()) && Integer.valueOf(1).equals(exam.getResultVisible())) {
                    vo.setTotalScore(record.getTotalScore());
                    vo.setPassed(record.getPassed());
                }
            }
            ExamPaper paper = paperMapper.selectById(exam.getPaperId());
            if (paper != null) {
                vo.setTotalPaperScore(paper.getTotalScore());
                vo.setPassScore(paper.getPassScore());
            }
            list.add(vo);
        }
        return list;
    }

    @Transactional
    /** 创建答卷、写入题目快照，返回题目（不含答案）和剩余秒数。 */
    public TakeVO start(Long examId) {
        Long studentId = SecurityUtils.requireStudentId();
        Exam exam = examMapper.selectById(examId);
        if (exam == null || "DRAFT".equals(exam.getStatus())) {
            throw new BizException("考试不存在");
        }
        ExamStudent es = examStudentMapper.selectOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId).eq(ExamStudent::getStudentId, studentId));
        if (es == null) {
            throw new BizException("你不在本场考试名单中");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new BizException("考试尚未开始");
        }
        if (now.isAfter(exam.getEndTime()) || "FINISHED".equals(exam.getStatus())) {
            throw new BizException("考试已结束");
        }
        ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getStudentId, studentId));
        if (record != null && !"ANSWERING".equals(record.getRecordStatus())) {
            throw new BizException("本场考试已交卷");
        }
        if (record == null) {
            record = new ExamRecord();
            record.setExamId(examId);
            record.setPaperId(exam.getPaperId());
            record.setStudentId(studentId);
            record.setStartTime(now);
            record.setObjectiveScore(BigDecimal.ZERO);
            record.setSubjectiveScore(BigDecimal.ZERO);
            record.setTotalScore(BigDecimal.ZERO);
            record.setPassed(0);
            record.setRecordStatus("ANSWERING");
            recordMapper.insert(record);
            createSnapshots(record);
            es.setExamStatus("ANSWERING");
            examStudentMapper.updateById(es);
        }
        if (remainingSeconds(exam, record) <= 0) {
            submitInternal(record, exam, "AUTO");
            throw new BizException("考试时间已到，系统已自动交卷");
        }
        return buildTakeVO(exam, record, false);
    }

    public TakeVO questions(Long recordId) {
        ExamRecord record = requireOwnRecord(recordId);
        Exam exam = examMapper.selectById(record.getExamId());
        boolean review = !"ANSWERING".equals(record.getRecordStatus());
        return buildTakeVO(exam, record, review);
    }

    @Transactional
    /** 考试中保存答案；时间到则自动交卷。 */
    public void saveAnswers(Long recordId, List<AnswerSaveRequest> answers) {
        ExamRecord record = requireOwnRecord(recordId);
        if (!"ANSWERING".equals(record.getRecordStatus())) {
            throw new BizException("已交卷，不能再修改");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (remainingSeconds(exam, record) <= 0) {
            submitInternal(record, exam, "AUTO");
            throw new BizException("考试时间已到，系统已自动交卷");
        }
        if (answers == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (AnswerSaveRequest item : answers) {
            ExamAnswer answer = answerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, recordId).eq(ExamAnswer::getQuestionId, item.getQuestionId()));
            if (answer == null) {
                continue;
            }
            answer.setStudentAnswer(item.getStudentAnswer());
            if (item.getFlagged() != null) {
                answer.setFlagged(item.getFlagged());
            }
            answer.setUpdatedAt(now);
            answerMapper.updateById(answer);
        }
    }

    @Transactional
    /** 交卷：先锁状态防重复，客观题自动评分，有简答题则 MARKING，否则 FINISHED。 */
    public void submit(Long recordId, List<AnswerSaveRequest> answers) {
        ExamRecord record = requireOwnRecord(recordId);
        if (!"ANSWERING".equals(record.getRecordStatus())) {
            throw new BizException("请勿重复交卷");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        long elapsed = Duration.between(record.getStartTime(), LocalDateTime.now()).toMinutes();
        if (exam.getAllowSubmitMinutes() != null && elapsed < exam.getAllowSubmitMinutes()) {
            throw new BizException("开考后 " + exam.getAllowSubmitMinutes() + " 分钟内不能交卷");
        }
        saveAnswersQuiet(record, answers);
        submitInternal(record, exam, "MANUAL");
    }

    public ReviewVO review(Long recordId) {
        ExamRecord record = requireOwnRecord(recordId);
        Exam exam = examMapper.selectById(record.getExamId());
        if ("ANSWERING".equals(record.getRecordStatus())) {
            throw new BizException("考试尚未交卷");
        }
        if (!Integer.valueOf(1).equals(exam.getResultVisible()) && !"FINISHED".equals(record.getRecordStatus())) {
            throw new BizException("成绩尚未公布");
        }
        ReviewVO vo = new ReviewVO();
        vo.setRecord(record);
        vo.setExamName(exam.getExamName());
        vo.setAnswerVisible(Integer.valueOf(1).equals(exam.getAnswerVisible()) && "FINISHED".equals(record.getRecordStatus()));
        vo.setMarking(!"FINISHED".equals(record.getRecordStatus()));
        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId).orderByAsc(ExamAnswer::getId));
        List<ReviewQuestionVO> qs = new ArrayList<>();
        for (ExamAnswer a : answers) {
            ReviewQuestionVO q = new ReviewQuestionVO();
            q.setQuestionId(a.getQuestionId());
            q.setContent(a.getQuestionContentSnapshot());
            q.setQuestionType(a.getQuestionTypeSnapshot());
            q.setStudentAnswer(a.getStudentAnswer());
            q.setQuestionScore(a.getQuestionScore());
            q.setScore("FINISHED".equals(record.getRecordStatus()) ? a.getScore() : null);
            q.setComment(a.getComment());
            if (vo.isAnswerVisible()) {
                q.setCorrectAnswer(a.getCorrectAnswerSnapshot());
                q.setIsCorrect(a.getIsCorrect());
                Question question = questionMapper.selectById(a.getQuestionId());
                if (question != null) {
                    q.setAnalysis(question.getAnalysis());
                }
            }
            q.setOptions(safeOptions(a.getQuestionId(), true));
            qs.add(q);
        }
        vo.setQuestions(qs);
        if (!"FINISHED".equals(record.getRecordStatus())) {
            record.setTotalScore(null);
            record.setPassed(null);
        }
        return vo;
    }

    private void saveAnswersQuiet(ExamRecord record, List<AnswerSaveRequest> answers) {
        if (answers == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (AnswerSaveRequest item : answers) {
            ExamAnswer answer = answerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId()).eq(ExamAnswer::getQuestionId, item.getQuestionId()));
            if (answer == null) {
                continue;
            }
            answer.setStudentAnswer(item.getStudentAnswer());
            answer.setUpdatedAt(now);
            answerMapper.updateById(answer);
        }
    }

    private void submitInternal(ExamRecord record, Exam exam, String submitType) {
        int rows = recordMapper.update(null, new LambdaUpdateWrapper<ExamRecord>()
                .eq(ExamRecord::getId, record.getId())
                .eq(ExamRecord::getRecordStatus, "ANSWERING")
                .set(ExamRecord::getRecordStatus, "SUBMITTED")
                .set(ExamRecord::getSubmitTime, LocalDateTime.now())
                .set(ExamRecord::getSubmitType, submitType));
        if (rows == 0) {
            throw new BizException("请勿重复交卷");
        }
        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()));
        BigDecimal objective = BigDecimal.ZERO;
        boolean hasEssay = false;
        for (ExamAnswer answer : answers) {
            String type = answer.getQuestionTypeSnapshot();
            if (ScoreCalculator.isSubjective(type)) {
                hasEssay = true;
                continue;
            }
            boolean ok = ScoreCalculator.match(type, answer.getStudentAnswer(), answer.getCorrectAnswerSnapshot());
            answer.setIsCorrect(ok ? 1 : 0);
            answer.setScore(ok ? answer.getQuestionScore() : BigDecimal.ZERO);
            answerMapper.updateById(answer);
            objective = objective.add(answer.getScore());
        }
        ExamRecord latest = recordMapper.selectById(record.getId());
        latest.setObjectiveScore(objective);
        ExamStudent es = examStudentMapper.selectOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, exam.getId()).eq(ExamStudent::getStudentId, record.getStudentId()));
        if (es != null) {
            es.setExamStatus("SUBMITTED");
            examStudentMapper.updateById(es);
        }
        if (hasEssay) {
            latest.setRecordStatus("MARKING");
            recordMapper.updateById(latest);
        } else {
            finishRecord(latest, exam, BigDecimal.ZERO);
        }
    }

    /** 阅卷结束或无简答题时：写总分、是否及格，并重算该生知识点掌握度。 */
    public void finishRecord(ExamRecord record, Exam exam, BigDecimal subjective) {
        ExamPaper paper = paperMapper.selectById(record.getPaperId());
        record.setSubjectiveScore(subjective);
        record.setTotalScore(record.getObjectiveScore().add(subjective));
        record.setPassed(record.getTotalScore().compareTo(paper.getPassScore()) >= 0 ? 1 : 0);
        record.setRecordStatus("FINISHED");
        recordMapper.updateById(record);
        knowledgeStatService.recalcStudent(record.getStudentId());
    }

    private void createSnapshots(ExamRecord record) {
        List<ExamPaperQuestion> items = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId())
                        .orderByAsc(ExamPaperQuestion::getSortNo));
        LocalDateTime now = LocalDateTime.now();
        for (ExamPaperQuestion item : items) {
            Question q = questionMapper.selectById(item.getQuestionId());
            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(item.getQuestionId());
            answer.setQuestionContentSnapshot(q.getContent());
            answer.setCorrectAnswerSnapshot(q.getCorrectAnswer());
            answer.setQuestionTypeSnapshot(q.getQuestionType());
            answer.setQuestionScore(item.getQuestionScore());
            answer.setScore(BigDecimal.ZERO);
            answer.setFlagged(0);
            answer.setUpdatedAt(now);
            answerMapper.insert(answer);
        }
    }

    private TakeVO buildTakeVO(Exam exam, ExamRecord record, boolean review) {
        TakeVO vo = new TakeVO();
        vo.setRecordId(record.getId());
        vo.setExamId(exam.getId());
        vo.setExamName(exam.getExamName());
        vo.setRecordStatus(record.getRecordStatus());
        vo.setRemainingSeconds(Math.max(0, remainingSeconds(exam, record)));
        vo.setServerTime(LocalDateTime.now());
        vo.setDurationMinutes(exam.getDurationMinutes());
        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()).orderByAsc(ExamAnswer::getId));
        List<TakeQuestionVO> qs = new ArrayList<>();
        int no = 1;
        for (ExamAnswer a : answers) {
            TakeQuestionVO q = new TakeQuestionVO();
            q.setQuestionId(a.getQuestionId());
            q.setSeq(no++);
            q.setContent(a.getQuestionContentSnapshot());
            q.setQuestionType(a.getQuestionTypeSnapshot());
            q.setQuestionScore(a.getQuestionScore());
            q.setStudentAnswer(a.getStudentAnswer());
            q.setFlagged(a.getFlagged());
            q.setOptions(safeOptions(a.getQuestionId(), review && Integer.valueOf(1).equals(exam.getAnswerVisible())));
            qs.add(q);
        }
        vo.setQuestions(qs);
        return vo;
    }

    private List<Map<String, Object>> safeOptions(Long questionId, boolean showCorrect) {
        List<QuestionOption> options = optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId).orderByAsc(QuestionOption::getSortNo));
        List<Map<String, Object>> list = new ArrayList<>();
        for (QuestionOption o : options) {
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("optionKey", o.getOptionKey());
            m.put("optionContent", o.getOptionContent());
            if (showCorrect) {
                m.put("isCorrect", o.getIsCorrect());
            }
            list.add(m);
        }
        return list;
    }

    private long remainingSeconds(Exam exam, ExamRecord record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime byDuration = record.getStartTime().plusMinutes(exam.getDurationMinutes());
        LocalDateTime deadline = byDuration.isBefore(exam.getEndTime()) ? byDuration : exam.getEndTime();
        return Duration.between(now, deadline).getSeconds();
    }

    private ExamRecord requireOwnRecord(Long recordId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null || !record.getStudentId().equals(SecurityUtils.requireStudentId())) {
            throw new BizException("答卷不存在");
        }
        return record;
    }

    @Data
    public static class StudentExamVO {
        private Long examId;
        private Long recordId;
        private String examName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private String runtimeStatus;
        private String examStatus;
        private String recordStatus;
        private Long remainingSeconds;
        private BigDecimal totalScore;
        private Integer passed;
        private BigDecimal totalPaperScore;
        private BigDecimal passScore;
    }

    @Data
    public static class TakeVO {
        private Long recordId;
        private Long examId;
        private String examName;
        private String recordStatus;
        private long remainingSeconds;
        private LocalDateTime serverTime;
        private Integer durationMinutes;
        private List<TakeQuestionVO> questions;
    }

    @Data
    public static class TakeQuestionVO {
        private Long questionId;
        private Integer seq;
        private String content;
        private String questionType;
        private BigDecimal questionScore;
        private String studentAnswer;
        private Integer flagged;
        private List<Map<String, Object>> options;
    }

    @Data
    public static class ReviewVO {
        private ExamRecord record;
        private String examName;
        private boolean answerVisible;
        private boolean marking;
        private List<ReviewQuestionVO> questions;
    }

    @Data
    public static class ReviewQuestionVO {
        private Long questionId;
        private String content;
        private String questionType;
        private String studentAnswer;
        private String correctAnswer;
        private String analysis;
        private BigDecimal questionScore;
        private BigDecimal score;
        private Integer isCorrect;
        private String comment;
        private List<Map<String, Object>> options;
    }
}
