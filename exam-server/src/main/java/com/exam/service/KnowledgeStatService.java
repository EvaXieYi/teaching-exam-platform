package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.ExamAnswer;
import com.exam.entity.ExamRecord;
import com.exam.entity.QuestionKnowledge;
import com.exam.entity.StudentKnowledgeStat;
import com.exam.mapper.ExamAnswerMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.QuestionKnowledgeMapper;
import com.exam.mapper.StudentKnowledgeStatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
/** 按学生已完成答卷重算 student_knowledge_stat。简答题按实际得分（不是只记对错）分摊到知识点。 */
public class KnowledgeStatService {
    private final ExamRecordMapper recordMapper;
    private final ExamAnswerMapper answerMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final StudentKnowledgeStatMapper statMapper;

    @Transactional
    public void recalcStudent(Long studentId) {
        statMapper.delete(new LambdaQueryWrapper<StudentKnowledgeStat>()
                .eq(StudentKnowledgeStat::getStudentId, studentId));
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getRecordStatus, "FINISHED"));
        if (records.isEmpty()) {
            return;
        }
        Map<Long, Agg> map = new HashMap<>();
        Set<Long> examIds = new HashSet<>();
        for (ExamRecord record : records) {
            examIds.add(record.getExamId());
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
                if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
                    totalWeight = BigDecimal.valueOf(links.size());
                }
                BigDecimal full = answer.getQuestionScore() == null ? BigDecimal.ZERO : answer.getQuestionScore();
                BigDecimal got = answer.getScore() == null ? BigDecimal.ZERO : answer.getScore();
                boolean fullScore = full.compareTo(BigDecimal.ZERO) > 0 && got.compareTo(full) >= 0;
                for (QuestionKnowledge link : links) {
                    BigDecimal w = link.getWeight() == null ? BigDecimal.ONE : link.getWeight();
                    BigDecimal ratio = w.divide(totalWeight, 6, RoundingMode.HALF_UP);
                    Agg agg = map.computeIfAbsent(link.getKnowledgePointId(), k -> new Agg());
                    agg.questionCount++;
                    agg.full = agg.full.add(full.multiply(ratio));
                    agg.got = agg.got.add(got.multiply(ratio));
                    if (fullScore) {
                        agg.correctCount++;
                    }
                    agg.lastExamId = record.getExamId();
                }
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, Agg> e : map.entrySet()) {
            Agg agg = e.getValue();
            StudentKnowledgeStat stat = new StudentKnowledgeStat();
            stat.setStudentId(studentId);
            stat.setKnowledgePointId(e.getKey());
            stat.setExamCount(examIds.size());
            stat.setQuestionCount(agg.questionCount);
            stat.setCorrectCount(agg.correctCount);
            stat.setGotScore(agg.got.setScale(2, RoundingMode.HALF_UP));
            stat.setFullScore(agg.full.setScale(2, RoundingMode.HALF_UP));
            if (agg.full.compareTo(BigDecimal.ZERO) > 0) {
                stat.setMasteryRate(agg.got.multiply(BigDecimal.valueOf(100))
                        .divide(agg.full, 2, RoundingMode.HALF_UP));
            } else {
                stat.setMasteryRate(BigDecimal.ZERO);
            }
            stat.setLastExamId(agg.lastExamId);
            stat.setUpdatedAt(now);
            statMapper.insert(stat);
        }
    }

    private static class Agg {
        int questionCount;
        int correctCount;
        BigDecimal got = BigDecimal.ZERO;
        BigDecimal full = BigDecimal.ZERO;
        Long lastExamId;
    }
}
