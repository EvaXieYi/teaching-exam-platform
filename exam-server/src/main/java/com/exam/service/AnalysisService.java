package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.Exam;
import com.exam.entity.ExamAnswer;
import com.exam.entity.ExamRecord;
import com.exam.entity.ExamStudent;
import com.exam.entity.KnowledgePoint;
import com.exam.entity.QuestionKnowledge;
import com.exam.entity.Student;
import com.exam.entity.StudentKnowledgeStat;
import com.exam.mapper.ExamAnswerMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ExamStudentMapper;
import com.exam.mapper.KnowledgePointMapper;
import com.exam.mapper.QuestionKnowledgeMapper;
import com.exam.mapper.StudentKnowledgeStatMapper;
import com.exam.mapper.StudentMapper;
import com.exam.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/** 学情分析：单场考试统计、学生/班级知识点掌握度。掌握度 = 实得分 / 满分 × 100。 */
public class AnalysisService {
    private final ExamMapper examMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final StudentMapper studentMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final StudentKnowledgeStatMapper statMapper;

    public Map<String, Object> examStatistics(Long examId) {
        Exam exam = examMapper.selectById(examId);
        List<ExamStudent> students = examStudentMapper.selectList(
                new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));
        List<ExamRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId));
        Map<String, Object> data = new HashMap<>();
        data.put("examName", exam.getExamName());
        data.put("assigned", students.size());
        long submitted = records.stream().filter(r -> !"ANSWERING".equals(r.getRecordStatus())).count();
        long finished = records.stream().filter(r -> "FINISHED".equals(r.getRecordStatus())).count();
        long marking = records.stream().filter(r -> "MARKING".equals(r.getRecordStatus())).count();
        data.put("submitted", submitted);
        data.put("finished", finished);
        data.put("marking", marking);
        data.put("absent", students.size() - submitted - records.stream().filter(r -> "ANSWERING".equals(r.getRecordStatus())).count());
        List<ExamRecord> scored = records.stream().filter(r -> "FINISHED".equals(r.getRecordStatus())).collect(Collectors.toList());
        if (!scored.isEmpty()) {
            BigDecimal sum = scored.stream().map(ExamRecord::getTotalScore).reduce(BigDecimal.ZERO, BigDecimal::add);
            data.put("avgScore", sum.divide(BigDecimal.valueOf(scored.size()), 2, RoundingMode.HALF_UP));
            long passed = scored.stream().filter(r -> Integer.valueOf(1).equals(r.getPassed())).count();
            data.put("passRate", BigDecimal.valueOf(passed * 100.0 / scored.size()).setScale(2, RoundingMode.HALF_UP));
        } else {
            data.put("avgScore", 0);
            data.put("passRate", 0);
        }
        List<Map<String, Object>> scoreList = new ArrayList<>();
        for (ExamStudent es : students) {
            Student s = studentMapper.selectById(es.getStudentId());
            ExamRecord rec = records.stream().filter(r -> r.getStudentId().equals(es.getStudentId())).findFirst().orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", es.getStudentId());
            row.put("studentNo", s == null ? "" : s.getStudentNo());
            row.put("studentName", s == null ? "" : s.getName());
            row.put("className", s == null ? "" : s.getClassName());
            row.put("examStatus", es.getExamStatus());
            if (rec != null) {
                row.put("recordStatus", rec.getRecordStatus());
                if ("FINISHED".equals(rec.getRecordStatus())) {
                    row.put("totalScore", rec.getTotalScore());
                    row.put("objectiveScore", rec.getObjectiveScore());
                    row.put("subjectiveScore", rec.getSubjectiveScore());
                    row.put("passed", rec.getPassed());
                }
                row.put("submitTime", rec.getSubmitTime());
            }
            scoreList.add(row);
        }
        data.put("students", scoreList);
        data.put("knowledge", examKnowledge(examId, records));
        return data;
    }

    public List<KnowledgeRow> studentKnowledge(Long studentId) {
        if (studentId == null) {
            studentId = SecurityUtils.requireStudentId();
        }
        List<StudentKnowledgeStat> stats = statMapper.selectList(new LambdaQueryWrapper<StudentKnowledgeStat>()
                .eq(StudentKnowledgeStat::getStudentId, studentId)
                .orderByAsc(StudentKnowledgeStat::getMasteryRate));
        return toRows(stats);
    }

    public List<KnowledgeRow> classKnowledge(String className) {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getClassName, className));
        if (students.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = students.stream().map(Student::getId).collect(Collectors.toList());
        List<StudentKnowledgeStat> stats = statMapper.selectList(new LambdaQueryWrapper<StudentKnowledgeStat>()
                .in(StudentKnowledgeStat::getStudentId, ids));
        Map<Long, Agg> map = new HashMap<>();
        for (StudentKnowledgeStat s : stats) {
            Agg agg = map.computeIfAbsent(s.getKnowledgePointId(), k -> new Agg());
            agg.got = agg.got.add(s.getGotScore());
            agg.full = agg.full.add(s.getFullScore());
            agg.questionCount += s.getQuestionCount();
        }
        List<KnowledgeRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Agg> e : map.entrySet()) {
            KnowledgePoint kp = knowledgePointMapper.selectById(e.getKey());
            KnowledgeRow row = new KnowledgeRow();
            row.setKnowledgePointId(e.getKey());
            row.setName(kp == null ? "" : kp.getName());
            Agg agg = e.getValue();
            row.setQuestionCount(agg.questionCount);
            row.setGotScore(agg.got);
            row.setFullScore(agg.full);
            if (agg.full.compareTo(BigDecimal.ZERO) > 0) {
                row.setMasteryRate(agg.got.multiply(BigDecimal.valueOf(100)).divide(agg.full, 2, RoundingMode.HALF_UP));
            } else {
                row.setMasteryRate(BigDecimal.ZERO);
            }
            row.setLevel(level(row.getMasteryRate()));
            rows.add(row);
        }
        rows.sort((a, b) -> a.getMasteryRate().compareTo(b.getMasteryRate()));
        return rows;
    }

    public List<ExamRecord> scoreList(Long examId, String className) {
        LambdaQueryWrapper<ExamRecord> w = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getRecordStatus, "FINISHED")
                .orderByDesc(ExamRecord::getTotalScore);
        if (examId != null) {
            w.eq(ExamRecord::getExamId, examId);
        }
        List<ExamRecord> records = recordMapper.selectList(w);
        if (className != null && !className.isEmpty()) {
            List<Long> ids = studentMapper.selectList(new LambdaQueryWrapper<Student>().eq(Student::getClassName, className))
                    .stream().map(Student::getId).collect(Collectors.toList());
            records = records.stream().filter(r -> ids.contains(r.getStudentId())).collect(Collectors.toList());
        }
        return records;
    }

    private List<KnowledgeRow> examKnowledge(Long examId, List<ExamRecord> records) {
        Map<Long, Agg> map = new HashMap<>();
        for (ExamRecord record : records) {
            if (!"FINISHED".equals(record.getRecordStatus())) {
                continue;
            }
            List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId()));
            for (ExamAnswer answer : answers) {
                List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                        new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, answer.getQuestionId()));
                if (links.isEmpty()) {
                    continue;
                }
                BigDecimal totalWeight = links.stream()
                        .map(l -> l.getWeight() == null ? BigDecimal.ONE : l.getWeight())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal full = answer.getQuestionScore() == null ? BigDecimal.ZERO : answer.getQuestionScore();
                BigDecimal got = answer.getScore() == null ? BigDecimal.ZERO : answer.getScore();
                for (QuestionKnowledge link : links) {
                    BigDecimal w = link.getWeight() == null ? BigDecimal.ONE : link.getWeight();
                    BigDecimal ratio = w.divide(totalWeight, 6, RoundingMode.HALF_UP);
                    Agg agg = map.computeIfAbsent(link.getKnowledgePointId(), k -> new Agg());
                    agg.full = agg.full.add(full.multiply(ratio));
                    agg.got = agg.got.add(got.multiply(ratio));
                    agg.questionCount++;
                }
            }
        }
        List<KnowledgeRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Agg> e : map.entrySet()) {
            KnowledgePoint kp = knowledgePointMapper.selectById(e.getKey());
            KnowledgeRow row = new KnowledgeRow();
            row.setKnowledgePointId(e.getKey());
            row.setName(kp == null ? "" : kp.getName());
            Agg agg = e.getValue();
            row.setGotScore(agg.got.setScale(2, RoundingMode.HALF_UP));
            row.setFullScore(agg.full.setScale(2, RoundingMode.HALF_UP));
            row.setQuestionCount(agg.questionCount);
            if (agg.full.compareTo(BigDecimal.ZERO) > 0) {
                row.setMasteryRate(agg.got.multiply(BigDecimal.valueOf(100)).divide(agg.full, 2, RoundingMode.HALF_UP));
            } else {
                row.setMasteryRate(BigDecimal.ZERO);
            }
            row.setLevel(level(row.getMasteryRate()));
            rows.add(row);
        }
        rows.sort((a, b) -> a.getMasteryRate().compareTo(b.getMasteryRate()));
        return rows;
    }

    private List<KnowledgeRow> toRows(List<StudentKnowledgeStat> stats) {
        List<KnowledgeRow> rows = new ArrayList<>();
        for (StudentKnowledgeStat s : stats) {
            KnowledgePoint kp = knowledgePointMapper.selectById(s.getKnowledgePointId());
            KnowledgeRow row = new KnowledgeRow();
            row.setKnowledgePointId(s.getKnowledgePointId());
            row.setName(kp == null ? "" : kp.getName());
            row.setGotScore(s.getGotScore());
            row.setFullScore(s.getFullScore());
            row.setQuestionCount(s.getQuestionCount());
            row.setMasteryRate(s.getMasteryRate());
            row.setLevel(level(s.getMasteryRate()));
            rows.add(row);
        }
        return rows;
    }

    private String level(BigDecimal rate) {
        if (rate.compareTo(new BigDecimal("80")) >= 0) {
            return "掌握";
        }
        if (rate.compareTo(new BigDecimal("60")) >= 0) {
            return "一般";
        }
        return "薄弱";
    }

    private static class Agg {
        BigDecimal got = BigDecimal.ZERO;
        BigDecimal full = BigDecimal.ZERO;
        int questionCount;
    }

    @Data
    public static class KnowledgeRow {
        private Long knowledgePointId;
        private String name;
        private Integer questionCount;
        private BigDecimal gotScore;
        private BigDecimal fullScore;
        private BigDecimal masteryRate;
        private String level;
    }
}
