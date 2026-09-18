package com.exam.service;

import com.exam.common.BizException;
import com.exam.dto.QuestionExportRequest;
import com.exam.entity.QuestionOption;
import com.exam.util.QuestionTypes;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
/**
 * 题库导出 PDF 练习卷：按题型分组编号，可选附答案与解析。
 * 中文字体按 exam.pdf-font-path → classpath:fonts/ → 系统字体目录的顺序查找，找到后缓存。
 */
public class QuestionPdfService {
    private static final String DEFAULT_TITLE = "云计算题库练习";
    private static final int MAX_TITLE_LENGTH = 60;
    /** 文件名非法字符：\ / : * ? " < > | 及控制字符。 */
    private static final String ILLEGAL_FILENAME_CHARS = "[\\\\/:*?\"<>|\\p{Cntrl}]";
    private static final String[] CN_NUM = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
    private static final float LINE_HEIGHT = 16f;
    private static final Color GRAY = new Color(0x66, 0x66, 0x66);
    private static final List<String> WINDOWS_FONTS = Arrays.asList(
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/msyh.ttc,0");
    private static final List<String> LINUX_FONTS = Arrays.asList(
            "/usr/share/fonts/google-noto-cjk/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/wqy-zenhei/wqy-zenhei.ttc,0");

    private final QuestionService questionService;
    private final String configuredFontPath;
    private volatile BaseFont cachedFont;

    public QuestionPdfService(QuestionService questionService,
                              @Value("${exam.pdf-font-path:}") String configuredFontPath) {
        this.questionService = questionService;
        this.configuredFontPath = configuredFontPath;
    }

    /** 标题为空时使用默认标题；过滤文件名非法字符并截断到 60 字，控制器用它拼下载文件名。 */
    public static String titleOf(QuestionExportRequest req) {
        if (req == null || !StringUtils.hasText(req.getTitle())) {
            return DEFAULT_TITLE;
        }
        String title = req.getTitle().trim().replaceAll(ILLEGAL_FILENAME_CHARS, "_");
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }
        return StringUtils.hasText(title) ? title : DEFAULT_TITLE;
    }

    public byte[] export(QuestionExportRequest req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new BizException(400, "请先选择题目");
        }
        List<QuestionService.QuestionVO> questions = questionService.listByIds(req.getIds());
        if (questions.isEmpty()) {
            throw new BizException(400, "所选题目不存在或已删除");
        }
        boolean withAnswer = Boolean.TRUE.equals(req.getWithAnswer());
        BaseFont bf = resolveFont();
        Font titleFont = new Font(bf, 18, Font.BOLD);
        Font subFont = new Font(bf, 10, Font.NORMAL, GRAY);
        Font groupFont = new Font(bf, 13, Font.BOLD);
        Font bodyFont = new Font(bf, 11, Font.NORMAL);
        Font answerFont = new Font(bf, 10, Font.NORMAL, GRAY);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph(titleOf(req), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph sub = new Paragraph("共 " + questions.size() + " 题　导出时间 "
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(14);
            doc.add(sub);

            int groupNo = 0;
            int questionNo = 0;
            for (Map.Entry<String, List<QuestionService.QuestionVO>> group : groupByType(questions).entrySet()) {
                String type = group.getKey();
                List<QuestionService.QuestionVO> list = group.getValue();
                Paragraph groupTitle = new Paragraph(cnNumber(groupNo++) + "、" + QuestionTypes.label(type)
                        + "（共 " + list.size() + " 题）", groupFont);
                groupTitle.setSpacingBefore(10);
                groupTitle.setSpacingAfter(6);
                doc.add(groupTitle);
                for (QuestionService.QuestionVO q : list) {
                    writeQuestion(doc, q, ++questionNo, withAnswer, bodyFont, answerFont);
                }
            }
        } catch (DocumentException e) {
            throw new BizException(500, "生成 PDF 失败：" + e.getMessage());
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    private void writeQuestion(Document doc, QuestionService.QuestionVO q, int no, boolean withAnswer,
                               Font bodyFont, Font answerFont) throws DocumentException {
        String type = q.getQuestionType();
        Paragraph stem = new Paragraph(no + ".（" + formatScore(q.getDefaultScore()) + "分）" + safe(q.getContent()), bodyFont);
        stem.setSpacingBefore(4);
        stem.setLeading(LINE_HEIGHT);
        doc.add(stem);

        if (QuestionTypes.isChoice(type) && q.getOptions() != null) {
            for (QuestionOption o : q.getOptions()) {
                Paragraph op = new Paragraph(safe(o.getOptionKey()) + ". " + safe(o.getOptionContent()), bodyFont);
                op.setIndentationLeft(18);
                op.setLeading(LINE_HEIGHT);
                doc.add(op);
            }
        }

        // 作答留白：填空 2 行，简答/关键词解释 6 行
        float blankLines = 0;
        if (QuestionTypes.FILL.equals(type)) {
            blankLines = 2;
        } else if (QuestionTypes.isSubjective(type)) {
            blankLines = 6;
        }

        if (withAnswer) {
            Paragraph answer = new Paragraph("【答案】" + displayAnswer(q), answerFont);
            answer.setIndentationLeft(18);
            answer.setLeading(LINE_HEIGHT - 2);
            answer.setSpacingBefore(2);
            doc.add(answer);
            if (StringUtils.hasText(q.getAnalysis())) {
                Paragraph analysis = new Paragraph("【解析】" + q.getAnalysis().trim(), answerFont);
                analysis.setIndentationLeft(18);
                analysis.setLeading(LINE_HEIGHT - 2);
                doc.add(analysis);
            }
        }

        Paragraph gap = new Paragraph(" ", bodyFont);
        gap.setSpacingAfter(blankLines * LINE_HEIGHT);
        doc.add(gap);
    }

    /** 判断题把选项 key 换成选项文字（如 A→正确），其他题型原样输出。 */
    private String displayAnswer(QuestionService.QuestionVO q) {
        String answer = safe(q.getCorrectAnswer());
        if (!QuestionTypes.JUDGE.equals(q.getQuestionType()) || q.getOptions() == null || answer.isEmpty()) {
            return answer.isEmpty() ? "（略）" : answer;
        }
        List<String> texts = new ArrayList<>();
        for (String key : answer.split("[,，、;；\\s]+")) {
            String k = key.trim();
            if (k.isEmpty()) {
                continue;
            }
            String text = q.getOptions().stream()
                    .filter(o -> k.equalsIgnoreCase(o.getOptionKey()))
                    .map(QuestionOption::getOptionContent)
                    .findFirst().orElse(k);
            texts.add(text);
        }
        return texts.isEmpty() ? answer : String.join("，", texts);
    }

    private Map<String, List<QuestionService.QuestionVO>> groupByType(List<QuestionService.QuestionVO> questions) {
        Map<String, List<QuestionService.QuestionVO>> groups = new LinkedHashMap<>();
        for (String type : QuestionTypes.ORDER) {
            groups.put(type, new ArrayList<>());
        }
        for (QuestionService.QuestionVO q : questions) {
            String type = q.getQuestionType() == null ? "" : q.getQuestionType().toUpperCase();
            groups.computeIfAbsent(type, k -> new ArrayList<>()).add(q);
        }
        groups.values().removeIf(List::isEmpty);
        return groups;
    }

    private String cnNumber(int index) {
        return index < CN_NUM.length ? CN_NUM[index] : String.valueOf(index + 1);
    }

    private String formatScore(BigDecimal score) {
        if (score == null) {
            return "0";
        }
        return score.stripTrailingZeros().toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------- 字体 ----------

    private BaseFont resolveFont() {
        BaseFont font = cachedFont;
        if (font != null) {
            return font;
        }
        synchronized (this) {
            if (cachedFont == null) {
                cachedFont = loadFont(locateFontPath());
            }
            return cachedFont;
        }
    }

    private BaseFont loadFont(String path) {
        try {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (DocumentException | IOException e) {
            throw new BizException(500, "加载 PDF 字体失败：" + path + "，" + e.getMessage());
        }
    }

    /** 依次尝试：配置项 → classpath:fonts/ → Windows 字体 → Linux 常见 CJK 字体。 */
    private String locateFontPath() {
        if (StringUtils.hasText(configuredFontPath)) {
            if (fontExists(configuredFontPath)) {
                return configuredFontPath.trim();
            }
            log.warn("配置的 PDF 字体不存在，将尝试其他字体：{}", configuredFontPath);
        }
        String classpathFont = classpathFont();
        if (classpathFont != null) {
            return classpathFont;
        }
        for (String candidate : WINDOWS_FONTS) {
            if (fontExists(candidate)) {
                return candidate;
            }
        }
        for (String candidate : LINUX_FONTS) {
            if (fontExists(candidate)) {
                return candidate;
            }
        }
        throw new BizException("服务器缺少中文字体，请配置 exam.pdf-font-path");
    }

    private String classpathFont() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:fonts/*.tt?");
            for (Resource r : resources) {
                String name = r.getFilename();
                if (name == null || !(name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".ttc"))) {
                    continue;
                }
                String path;
                if (r.isFile()) {
                    path = r.getFile().getAbsolutePath();
                } else {
                    // jar 内资源无法直接按路径读取，复制到临时文件
                    Path tmp = Files.createTempFile("exam-pdf-font-", "-" + name);
                    try (InputStream in = r.getInputStream()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    tmp.toFile().deleteOnExit();
                    path = tmp.toAbsolutePath().toString();
                }
                return name.toLowerCase().endsWith(".ttc") ? path + ",0" : path;
            }
        } catch (IOException e) {
            log.warn("扫描 classpath:fonts 失败：{}", e.getMessage());
        }
        return null;
    }

    /** 判断存在性时去掉 .ttc 的 ",0" 子字体后缀。 */
    private boolean fontExists(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String real = path.trim().replaceAll(",\\d+$", "");
        try {
            return Files.exists(Paths.get(real));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
