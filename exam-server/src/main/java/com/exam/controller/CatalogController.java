package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.KnowledgePoint;
import com.exam.entity.QuestionCategory;
import com.exam.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping("/api/knowledge-points/tree")
    public Result<List<CatalogService.TreeNode>> knowledgeTree() {
        return Result.ok(catalogService.knowledgeTree());
    }

    @PostMapping("/api/knowledge-points")
    public Result<KnowledgePoint> createKp(@RequestBody KnowledgePoint point) {
        point.setId(null);
        return Result.ok(catalogService.saveKnowledge(point));
    }

    @PutMapping("/api/knowledge-points/{id}")
    public Result<KnowledgePoint> updateKp(@PathVariable Long id, @RequestBody KnowledgePoint point) {
        point.setId(id);
        return Result.ok(catalogService.saveKnowledge(point));
    }

    @DeleteMapping("/api/knowledge-points/{id}")
    public Result<Void> deleteKp(@PathVariable Long id) {
        catalogService.deleteKnowledge(id);
        return Result.ok();
    }

    @GetMapping("/api/question-categories")
    public Result<List<QuestionCategory>> categories() {
        return Result.ok(catalogService.listCategories());
    }

    @PostMapping("/api/question-categories")
    public Result<QuestionCategory> createCategory(@RequestBody QuestionCategory category) {
        category.setId(null);
        return Result.ok(catalogService.saveCategory(category));
    }

    @PutMapping("/api/question-categories/{id}")
    public Result<QuestionCategory> updateCategory(@PathVariable Long id, @RequestBody QuestionCategory category) {
        category.setId(id);
        return Result.ok(catalogService.saveCategory(category));
    }

    @DeleteMapping("/api/question-categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return Result.ok();
    }
}
