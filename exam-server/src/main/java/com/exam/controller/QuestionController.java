package com.exam.controller;

import com.alibaba.excel.EasyExcel;
import com.exam.common.BizException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.QuestionExportRequest;
import com.exam.dto.QuestionImportRow;
import com.exam.dto.QuestionSaveRequest;
import com.exam.service.QuestionImportService;
import com.exam.service.QuestionPdfService;
import com.exam.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 题库增删改查，支持单选/多选/判断/填空/简答/关键词解释；另提供 Excel 批量导入与 PDF 练习卷导出。 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;
    private final QuestionImportService questionImportService;
    private final QuestionPdfService questionPdfService;
    /** 单次导入行数上限，避免超大文件拖垮解析与事务。 */
    private static final int MAX_IMPORT_ROWS = 2000;

    @GetMapping
    public Result<PageResult<QuestionService.QuestionVO>> page(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               @RequestParam(required = false) String type,
                                                               @RequestParam(required = false) Long categoryId,
                                                               @RequestParam(required = false) Long knowledgePointId,
                                                               @RequestParam(required = false) String keyword) {
        return Result.ok(questionService.page(page, size, type, categoryId, knowledgePointId, keyword));
    }

    /** 下载导入模板（示例数据 + 填写说明）。字面路径优先于下方的 /{id}。 */
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        setAttachment(response, "题库导入模板.xlsx");
        questionImportService.writeTemplate(response.getOutputStream());
    }

    /** Excel 批量导入，逐行返回失败原因；知识点列留空的行使用 defaultKnowledgePointId。 */
    @PostMapping("/import")
    public Result<QuestionImportService.ImportResult> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long defaultKnowledgePointId) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            throw new BizException("请上传 .xlsx 或 .xls 文件");
        }
        List<QuestionImportRow> rows = EasyExcel.read(file.getInputStream())
                .head(QuestionImportRow.class).sheet(0).doReadSync();
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BizException("一次导入最多 " + MAX_IMPORT_ROWS + " 行，请拆分文件");
        }
        return Result.ok(questionImportService.importQuestions(rows, defaultKnowledgePointId));
    }

    /** 选中题目导出为 PDF 练习卷。 */
    @PostMapping("/export-pdf")
    public void exportPdf(@RequestBody QuestionExportRequest req, HttpServletResponse response) throws IOException {
        byte[] pdf = questionPdfService.export(req);
        response.setContentType("application/pdf");
        setAttachment(response, QuestionPdfService.titleOf(req) + ".pdf");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.flushBuffer();
    }

    @GetMapping("/{id}")
    public Result<QuestionService.QuestionVO> detail(@PathVariable Long id) {
        return Result.ok(questionService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody QuestionSaveRequest req) {
        req.setId(null);
        return Result.ok(questionService.save(req));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody QuestionSaveRequest req) {
        req.setId(id);
        return Result.ok(questionService.save(req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    /** 中文文件名同时给 filename 与 filename*，并暴露 Content-Disposition 供前端读取。 */
    private void setAttachment(HttpServletResponse response, String fileName) throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encoded + ";filename*=UTF-8''" + encoded);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
