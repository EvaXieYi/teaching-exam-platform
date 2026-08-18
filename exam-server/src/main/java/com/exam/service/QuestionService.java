package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.common.PageResult;
import com.exam.dto.QuestionSaveRequest;
import com.exam.entity.KnowledgePoint;
import com.exam.entity.Question;
import com.exam.entity.QuestionKnowledge;
import com.exam.entity.QuestionOption;
import com.exam.mapper.KnowledgePointMapper;
import com.exam.mapper.QuestionKnowledgeMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionOptionMapper;
import com.exam.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public PageResult<QuestionVO> page(long page, long size, String type, Long categoryId, Long knowledgePointId, String keyword) {
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<Question>().eq(Question::getStatus, 1);
        if (!SecurityUtils.isAdmin()) {
            Long uid = SecurityUtils.requireUser().getUserId();
            w.and(q -> q.eq(Question::getVisibility, "PUBLIC").or().eq(Question::getCreatedBy, uid));
        }
        if (StringUtils.hasText(type)) {
            w.eq(Question::getQuestionType, type);
        }
        if (categoryId != null) {
            w.eq(Question::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            w.like(Question::getContent, keyword);
        }
        if (knowledgePointId != null) {
            List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                    new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getKnowledgePointId, knowledgePointId));
            if (links.isEmpty()) {
                return PageResult.of(0, new ArrayList<>());
            }
            w.in(Question::getId, links.stream().map(QuestionKnowledge::getQuestionId).collect(Collectors.toList()));
        }
        w.orderByDesc(Question::getId);
        Page<Question> p = questionMapper.selectPage(new Page<>(page, size), w);
        List<QuestionVO> records = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), records);
    }

    public QuestionVO detail(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null || question.getStatus() == 0) {
            throw new BizException("题目不存在");
        }
        return toVO(question);
    }

    @Transactional
    public Long save(QuestionSaveRequest req) {
        if (req.getKnowledgePointIds() == null || req.getKnowledgePointIds().isEmpty()) {
            throw new BizException("请至少绑定一个知识点");
        }
        if (!StringUtils.hasText(req.getContent())) {
            throw new BizException("请填写题干");
        }
        LocalDateTime now = LocalDateTime.now();
        Question question = req.getId() == null ? new Question() : questionMapper.selectById(req.getId());
        if (question == null) {
            throw new BizException("题目不存在");
        }
        question.setCategoryId(req.getCategoryId());
        question.setQuestionType(req.getQuestionType());
        question.setContent(req.getContent());
        question.setAnalysis(req.getAnalysis());
        question.setDifficulty(req.getDifficulty() == null ? 1 : req.getDifficulty());
        question.setDefaultScore(req.getDefaultScore() == null ? BigDecimal.ONE : req.getDefaultScore());
        question.setVisibility(StringUtils.hasText(req.getVisibility()) ? req.getVisibility() : "PUBLIC");
        question.setStatus(1);
        question.setUpdatedAt(now);
        fillCorrectAnswer(question, req);
        if (req.getId() == null) {
            question.setCreatedBy(SecurityUtils.requireUser().getUserId());
            question.setCreatedAt(now);
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
            optionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, question.getId()));
            questionKnowledgeMapper.delete(new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, question.getId()));
        }
        if (req.getOptions() != null) {
            int i = 0;
            for (QuestionSaveRequest.OptionItem item : req.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setOptionKey(item.getOptionKey());
                option.setOptionContent(item.getOptionContent());
                option.setIsCorrect(item.getIsCorrect() == null ? 0 : item.getIsCorrect());
                option.setSortNo(item.getSortNo() == null ? i : item.getSortNo());
                optionMapper.insert(option);
                i++;
            }
        }
        for (Long kpId : req.getKnowledgePointIds()) {
            QuestionKnowledge link = new QuestionKnowledge();
            link.setQuestionId(question.getId());
            link.setKnowledgePointId(kpId);
            link.setWeight(BigDecimal.ONE);
            questionKnowledgeMapper.insert(link);
        }
        return question.getId();
    }

    public void delete(Long id) {
        Question question = questionMapper.selectById(id);
        if (question != null) {
            question.setStatus(0);
            questionMapper.updateById(question);
        }
    }

    private void fillCorrectAnswer(Question question, QuestionSaveRequest req) {
        String type = req.getQuestionType();
        if ("SINGLE".equals(type) || "MULTIPLE".equals(type) || "JUDGE".equals(type)) {
            if (req.getOptions() == null) {
                throw new BizException("请填写选项");
            }
            String answer = req.getOptions().stream()
                    .filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1)
                    .map(QuestionSaveRequest.OptionItem::getOptionKey)
                    .collect(Collectors.joining(","));
            if (!StringUtils.hasText(answer)) {
                throw new BizException("请设置正确答案");
            }
            question.setCorrectAnswer(answer);
        } else {
            question.setCorrectAnswer(req.getCorrectAnswer());
        }
    }

    private QuestionVO toVO(Question question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setCategoryId(question.getCategoryId());
        vo.setQuestionType(question.getQuestionType());
        vo.setContent(question.getContent());
        vo.setCorrectAnswer(question.getCorrectAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setDifficulty(question.getDifficulty());
        vo.setDefaultScore(question.getDefaultScore());
        vo.setVisibility(question.getVisibility());
        vo.setCreatedBy(question.getCreatedBy());
        vo.setCreatedAt(question.getCreatedAt());
        vo.setOptions(optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, question.getId())
                .orderByAsc(QuestionOption::getSortNo)));
        List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, question.getId()));
        vo.setKnowledgePointIds(links.stream().map(QuestionKnowledge::getKnowledgePointId).collect(Collectors.toList()));
        if (!links.isEmpty()) {
            List<KnowledgePoint> kps = knowledgePointMapper.selectBatchIds(vo.getKnowledgePointIds());
            Map<Long, String> names = kps.stream().collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName));
            vo.setKnowledgePointNames(vo.getKnowledgePointIds().stream().map(names::get).collect(Collectors.toList()));
        }
        return vo;
    }

    @Data
    public static class QuestionVO {
        private Long id;
        private Long categoryId;
        private String questionType;
        private String content;
        private String correctAnswer;
        private String analysis;
        private Integer difficulty;
        private BigDecimal defaultScore;
        private String visibility;
        private Long createdBy;
        private java.time.LocalDateTime createdAt;
        private List<QuestionOption> options;
        private List<Long> knowledgePointIds;
        private List<String> knowledgePointNames;
    }
}
