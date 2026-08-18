package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BizException;
import com.exam.common.PageResult;
import com.exam.dto.PaperSaveRequest;
import com.exam.entity.ExamPaper;
import com.exam.entity.ExamPaperQuestion;
import com.exam.entity.Question;
import com.exam.entity.QuestionKnowledge;
import com.exam.mapper.ExamPaperMapper;
import com.exam.mapper.ExamPaperQuestionMapper;
import com.exam.mapper.QuestionKnowledgeMapper;
import com.exam.mapper.QuestionMapper;
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
public class PaperService {
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;

    public PageResult<ExamPaper> page(long page, long size, String keyword) {
        LambdaQueryWrapper<ExamPaper> w = new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            w.like(ExamPaper::getPaperName, keyword);
        }
        if (!SecurityUtils.isAdmin()) {
            w.eq(ExamPaper::getCreatedBy, SecurityUtils.requireUser().getUserId());
        }
        w.orderByDesc(ExamPaper::getId);
        Page<ExamPaper> p = paperMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public PaperVO detail(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BizException("试卷不存在");
        }
        PaperVO vo = new PaperVO();
        vo.setPaper(paper);
        List<ExamPaperQuestion> items = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, id)
                        .orderByAsc(ExamPaperQuestion::getSortNo));
        List<PaperQuestionVO> questions = new ArrayList<>();
        for (ExamPaperQuestion item : items) {
            Question q = questionMapper.selectById(item.getQuestionId());
            PaperQuestionVO qv = new PaperQuestionVO();
            qv.setQuestionId(item.getQuestionId());
            qv.setQuestionScore(item.getQuestionScore());
            qv.setSortNo(item.getSortNo());
            if (q != null) {
                qv.setContent(q.getContent());
                qv.setQuestionType(q.getQuestionType());
            }
            questions.add(qv);
        }
        vo.setQuestions(questions);
        return vo;
    }

    @Transactional
    public Long save(PaperSaveRequest req) {
        if (req.getQuestions() == null || req.getQuestions().isEmpty()) {
            throw new BizException("试卷至少包含一道题");
        }
        for (PaperSaveRequest.Item item : req.getQuestions()) {
            Long kpCount = questionKnowledgeMapper.selectCount(new LambdaQueryWrapper<QuestionKnowledge>()
                    .eq(QuestionKnowledge::getQuestionId, item.getQuestionId()));
            if (kpCount == 0) {
                throw new BizException("存在未绑定知识点的题目，不能组卷");
            }
            Question q = questionMapper.selectById(item.getQuestionId());
            if (q == null || q.getStatus() == 0) {
                throw new BizException("题目不存在或已删除");
            }
        }
        BigDecimal total = req.getQuestions().stream()
                .map(i -> i.getQuestionScore() == null ? BigDecimal.ZERO : i.getQuestionScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        ExamPaper paper = req.getId() == null ? new ExamPaper() : paperMapper.selectById(req.getId());
        if (paper == null) {
            throw new BizException("试卷不存在");
        }
        paper.setPaperName(req.getPaperName());
        paper.setTotalScore(total);
        paper.setPassScore(req.getPassScore() == null ? total.multiply(new BigDecimal("0.6")) : req.getPassScore());
        paper.setQuestionCount(req.getQuestions().size());
        paper.setStatus(1);
        paper.setUpdatedAt(now);
        if (req.getId() == null) {
            paper.setCreatedBy(SecurityUtils.requireUser().getUserId());
            paper.setCreatedAt(now);
            paperMapper.insert(paper);
        } else {
            paperMapper.updateById(paper);
            paperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paper.getId()));
        }
        int sort = 1;
        for (PaperSaveRequest.Item item : req.getQuestions()) {
            ExamPaperQuestion row = new ExamPaperQuestion();
            row.setPaperId(paper.getId());
            row.setQuestionId(item.getQuestionId());
            row.setQuestionScore(item.getQuestionScore());
            row.setSortNo(item.getSortNo() == null ? sort : item.getSortNo());
            paperQuestionMapper.insert(row);
            sort++;
        }
        return paper.getId();
    }

    public void delete(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper != null) {
            paper.setStatus(0);
            paperMapper.updateById(paper);
        }
    }

    public List<ExamPaper> options() {
        LambdaQueryWrapper<ExamPaper> w = new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getStatus, 1);
        if (!SecurityUtils.isAdmin()) {
            w.eq(ExamPaper::getCreatedBy, SecurityUtils.requireUser().getUserId());
        }
        return paperMapper.selectList(w.orderByDesc(ExamPaper::getId));
    }

    @Data
    public static class PaperVO {
        private ExamPaper paper;
        private List<PaperQuestionVO> questions;
    }

    @Data
    public static class PaperQuestionVO {
        private Long questionId;
        private String content;
        private String questionType;
        private BigDecimal questionScore;
        private Integer sortNo;
    }
}
