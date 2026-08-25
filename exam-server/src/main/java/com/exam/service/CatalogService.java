package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BizException;
import com.exam.entity.KnowledgePoint;
import com.exam.entity.QuestionCategory;
import com.exam.mapper.KnowledgePointMapper;
import com.exam.mapper.QuestionCategoryMapper;
import com.exam.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/** 知识点树、题库分类的维护。 */
public class CatalogService {
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionCategoryMapper categoryMapper;

    public List<TreeNode> knowledgeTree() {
        List<KnowledgePoint> all = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getStatus, 1)
                        .orderByAsc(KnowledgePoint::getSortNo).orderByAsc(KnowledgePoint::getId));
        return buildKp(all, 0L);
    }

    public KnowledgePoint saveKnowledge(KnowledgePoint point) {
        LocalDateTime now = LocalDateTime.now();
        if (point.getParentId() == null) {
            point.setParentId(0L);
        }
        if (!StringUtils.hasText(point.getName())) {
            throw new BizException(point.getParentId() == 0L ? "请填写分类名称" : "请填写知识点名称");
        }
        if (point.getStatus() == null) {
            point.setStatus(1);
        }
        point.setUpdatedAt(now);
        if (point.getId() == null) {
            point.setCreatedBy(SecurityUtils.requireUser().getUserId());
            point.setCreatedAt(now);
            knowledgePointMapper.insert(point);
        } else {
            knowledgePointMapper.updateById(point);
        }
        return point;
    }

    public void deleteKnowledge(Long id) {
        Long child = knowledgePointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getParentId, id).eq(KnowledgePoint::getStatus, 1));
        if (child > 0) {
            throw new BizException("请先删除子知识点");
        }
        KnowledgePoint point = knowledgePointMapper.selectById(id);
        if (point != null) {
            point.setStatus(0);
            knowledgePointMapper.updateById(point);
        }
    }

    /** 题库「分类」= 知识点树根节点（parentId=0）。在知识点页新增分类后，出题下拉即可看到。 */
    public List<QuestionCategory> listCategories() {
        List<KnowledgePoint> roots = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getParentId, 0L)
                        .eq(KnowledgePoint::getStatus, 1)
                        .orderByAsc(KnowledgePoint::getSortNo)
                        .orderByAsc(KnowledgePoint::getId));
        List<QuestionCategory> list = new ArrayList<>();
        for (KnowledgePoint p : roots) {
            QuestionCategory c = new QuestionCategory();
            c.setId(p.getId());
            c.setCategoryName(p.getName());
            c.setParentId(0L);
            c.setSortNo(p.getSortNo());
            list.add(c);
        }
        return list;
    }

    public QuestionCategory saveCategory(QuestionCategory category) {
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getId() == null) {
            category.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    private List<TreeNode> buildKp(List<KnowledgePoint> all, Long parentId) {
        Map<Long, List<KnowledgePoint>> grouped = all.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));
        return toNodes(grouped, parentId);
    }

    private List<TreeNode> toNodes(Map<Long, List<KnowledgePoint>> grouped, Long parentId) {
        List<KnowledgePoint> children = grouped.getOrDefault(parentId, new ArrayList<>());
        List<TreeNode> nodes = new ArrayList<>();
        for (KnowledgePoint p : children) {
            TreeNode node = new TreeNode();
            node.setId(p.getId());
            node.setName(p.getName());
            node.setParentId(p.getParentId());
            node.setCode(p.getCode());
            node.setSortNo(p.getSortNo());
            node.setChildren(toNodes(grouped, p.getId()));
            nodes.add(node);
        }
        return nodes;
    }

    @Data
    public static class TreeNode {
        private Long id;
        private String name;
        private Long parentId;
        private String code;
        private Integer sortNo;
        private List<TreeNode> children;
    }
}
