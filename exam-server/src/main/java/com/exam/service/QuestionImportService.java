package com.exam.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BizException;
import com.exam.dto.QuestionImportRow;
import com.exam.dto.QuestionSaveRequest;
import com.exam.entity.KnowledgePoint;
import com.exam.mapper.KnowledgePointMapper;
import com.exam.util.QuestionTypes;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * 题库 Excel 导入：逐行校验后调用 QuestionService.save，单行失败不影响其他行，结果里带上每行错误。
 * 同时负责生成带示例与填写说明的导入模板。
 */
public class QuestionImportService {
    private static final String[] OPTION_KEYS = {"A", "B", "C", "D", "E", "F"};

    private final QuestionService questionService;
    private final KnowledgePointMapper knowledgePointMapper;

    /** rows 为去掉表头后的数据行，rowNo 从 2 起对应 Excel 行号；不加事务，成功的行直接落库。 */
    public ImportResult importQuestions(List<QuestionImportRow> rows, Long defaultKnowledgePointId) {
        ImportResult result = new ImportResult();
        if (rows == null) {
            return result;
        }
        for (int i = 0; i < rows.size(); i++) {
            QuestionImportRow row = rows.get(i);
            int rowNo = i + 2;
            if (row == null || (!StringUtils.hasText(row.getQuestionType()) && !StringUtils.hasText(row.getContent()))) {
                continue;
            }
            result.total++;
            try {
                questionService.save(toRequest(row, defaultKnowledgePointId));
                result.success++;
            } catch (BizException e) {
                result.fail(rowNo, e.getMessage());
            } catch (Exception e) {
                result.fail(rowNo, "导入失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
        return result;
    }

    private QuestionSaveRequest toRequest(QuestionImportRow row, Long defaultKnowledgePointId) {
        String type = QuestionTypes.fromLabel(row.getQuestionType());
        if (type == null) {
            throw new BizException("题型不识别：" + trim(row.getQuestionType()));
        }
        String content = trim(row.getContent());
        if (!StringUtils.hasText(content)) {
            throw new BizException("请填写题干");
        }
        List<Long> kpIds = resolveKnowledgePoints(row.getKnowledgePoints(), defaultKnowledgePointId);

        QuestionSaveRequest req = new QuestionSaveRequest();
        req.setQuestionType(type);
        req.setContent(content);
        req.setAnalysis(trim(row.getAnalysis()));
        req.setDifficulty(parseDifficulty(row.getDifficulty()));
        req.setDefaultScore(parseScore(row.getDefaultScore(), type));
        req.setVisibility(parseVisibility(row.getVisibility()));
        req.setKnowledgePointIds(kpIds);
        req.setCategoryId(rootOf(kpIds.get(0)));

        if (QuestionTypes.isChoice(type)) {
            req.setOptions(buildOptions(row, type));
        } else if (QuestionTypes.FILL.equals(type)) {
            String answer = trim(row.getCorrectAnswer());
            if (!StringUtils.hasText(answer)) {
                throw new BizException("填空题必须填写正确答案");
            }
            req.setCorrectAnswer(answer);
        } else {
            req.setCorrectAnswer(trim(row.getCorrectAnswer()));
        }
        return req;
    }

    // ---------- 选择题选项与答案 ----------

    private List<QuestionSaveRequest.OptionItem> buildOptions(QuestionImportRow row, String type) {
        String[] raw = {row.getOptionA(), row.getOptionB(), row.getOptionC(),
                row.getOptionD(), row.getOptionE(), row.getOptionF()};
        List<QuestionSaveRequest.OptionItem> options = new ArrayList<>();
        int sort = 1;
        for (int i = 0; i < raw.length; i++) {
            if (StringUtils.hasText(raw[i])) {
                options.add(option(OPTION_KEYS[i], raw[i].trim(), sort++));
            }
        }
        if (options.isEmpty() && QuestionTypes.JUDGE.equals(type)) {
            options.add(option("A", "正确", 1));
            options.add(option("B", "错误", 2));
        }
        if (options.size() < 2) {
            throw new BizException("选择题至少需要 2 个选项");
        }
        Set<String> answers = QuestionTypes.JUDGE.equals(type)
                ? parseJudgeAnswer(row.getCorrectAnswer())
                : parseChoiceAnswer(row.getCorrectAnswer());
        if (QuestionTypes.SINGLE.equals(type) && answers.size() != 1) {
            throw new BizException("单选题正确答案必须且只能有 1 个");
        }
        if (QuestionTypes.MULTIPLE.equals(type) && answers.size() < 2) {
            throw new BizException("多选题正确答案至少 2 个");
        }
        for (String a : answers) {
            boolean exists = options.stream().anyMatch(o -> o.getOptionKey().equals(a));
            if (!exists) {
                throw new BizException("正确答案 " + a + " 对应的选项不存在");
            }
        }
        for (QuestionSaveRequest.OptionItem o : options) {
            o.setIsCorrect(answers.contains(o.getOptionKey()) ? 1 : 0);
        }
        return options;
    }

    private QuestionSaveRequest.OptionItem option(String key, String content, int sort) {
        QuestionSaveRequest.OptionItem item = new QuestionSaveRequest.OptionItem();
        item.setOptionKey(key);
        item.setOptionContent(content);
        item.setIsCorrect(0);
        item.setSortNo(sort);
        return item;
    }

    /** 接受 ABD / A,B,D / A、B、D / A;B;D，统一成大写字母集合。 */
    private Set<String> parseChoiceAnswer(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[\\s,，、;；]+", "").toUpperCase();
        if (cleaned.isEmpty()) {
            throw new BizException("请填写正确答案");
        }
        Set<String> answers = new LinkedHashSet<>();
        for (char c : cleaned.toCharArray()) {
            if (c < 'A' || c > 'F') {
                throw new BizException("正确答案格式不正确：" + raw.trim());
            }
            answers.add(String.valueOf(c));
        }
        return answers;
    }

    /** 判断题答案：对/正确/是/T/TRUE/A → A，错/错误/否/F/FALSE/B → B。 */
    private Set<String> parseJudgeAnswer(String raw) {
        String t = raw == null ? "" : raw.replaceAll("\\s+", "").toUpperCase();
        if (t.isEmpty()) {
            throw new BizException("请填写正确答案");
        }
        switch (t) {
            case "对":
            case "正确":
            case "是":
            case "T":
            case "TRUE":
            case "A":
                return Collections.singleton("A");
            case "错":
            case "错误":
            case "否":
            case "F":
            case "FALSE":
            case "B":
                return Collections.singleton("B");
            default:
                throw new BizException("判断题正确答案请填写 对/错：" + raw.trim());
        }
    }

    // ---------- 知识点 ----------

    /** 知识点列多项以 | 分隔；含 / 的按路径逐级匹配，否则按名称精确匹配；留空则用默认知识点。 */
    private List<Long> resolveKnowledgePoints(String raw, Long defaultKnowledgePointId) {
        if (!StringUtils.hasText(raw)) {
            if (defaultKnowledgePointId == null) {
                throw new BizException("未填写知识点且未选择默认知识点");
            }
            KnowledgePoint kp = knowledgePointMapper.selectById(defaultKnowledgePointId);
            if (kp == null || kp.getStatus() == null || kp.getStatus() != 1) {
                throw new BizException("默认知识点不存在或已停用");
            }
            return new ArrayList<>(Collections.singletonList(kp.getId()));
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String item : raw.split("\\|")) {
            String name = item.trim();
            if (name.isEmpty()) {
                continue;
            }
            ids.add(name.contains("/") ? resolveByPath(name) : resolveByName(name));
        }
        if (ids.isEmpty()) {
            throw new BizException("未填写知识点且未选择默认知识点");
        }
        return new ArrayList<>(ids);
    }

    private Long resolveByPath(String path) {
        Long parentId = 0L;
        KnowledgePoint current = null;
        for (String seg : path.split("/")) {
            String name = seg.trim();
            if (name.isEmpty()) {
                continue;
            }
            List<KnowledgePoint> found = knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                    .eq(KnowledgePoint::getParentId, parentId)
                    .eq(KnowledgePoint::getName, name)
                    .eq(KnowledgePoint::getStatus, 1));
            if (found.isEmpty()) {
                throw new BizException("知识点不存在：" + path);
            }
            if (found.size() > 1) {
                throw new BizException("知识点名称歧义，请使用完整路径：" + path);
            }
            current = found.get(0);
            parentId = current.getId();
        }
        if (current == null) {
            throw new BizException("知识点不存在：" + path);
        }
        return current.getId();
    }

    private Long resolveByName(String name) {
        List<KnowledgePoint> found = knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getName, name)
                .eq(KnowledgePoint::getStatus, 1));
        if (found.isEmpty()) {
            throw new BizException("知识点不存在：" + name);
        }
        if (found.size() > 1) {
            throw new BizException("知识点名称歧义，请使用完整路径：" + name);
        }
        return found.get(0).getId();
    }

    /** 沿 parentId 向上找到根节点 id 作为 categoryId。 */
    private Long rootOf(Long kpId) {
        KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
        int guard = 0;
        while (kp != null && kp.getParentId() != null && kp.getParentId() != 0L && guard++ < 50) {
            KnowledgePoint parent = knowledgePointMapper.selectById(kp.getParentId());
            if (parent == null) {
                break;
            }
            kp = parent;
        }
        return kp == null ? kpId : kp.getId();
    }

    // ---------- 其他列 ----------

    private Integer parseDifficulty(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 1;
        }
        try {
            BigDecimal d = new BigDecimal(raw.trim());
            if (d.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            int v = d.intValueExact();
            if (v < 1 || v > 3) {
                throw new NumberFormatException();
            }
            return v;
        } catch (ArithmeticException | NumberFormatException e) {
            throw new BizException("难度必须是 1-3 的整数：" + raw.trim());
        }
    }

    private BigDecimal parseScore(String raw, String type) {
        if (!StringUtils.hasText(raw)) {
            return QuestionTypes.defaultScore(type);
        }
        try {
            BigDecimal score = new BigDecimal(raw.trim());
            if (score.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("默认分值必须大于 0：" + raw.trim());
            }
            return score;
        } catch (NumberFormatException e) {
            throw new BizException("默认分值必须是数字：" + raw.trim());
        }
    }

    private String parseVisibility(String raw) {
        String t = trim(raw);
        if (!StringUtils.hasText(t) || "公开".equals(t) || "PUBLIC".equalsIgnoreCase(t)) {
            return "PUBLIC";
        }
        if ("私有".equals(t) || "PRIVATE".equalsIgnoreCase(t)) {
            return "PRIVATE";
        }
        throw new BizException("可见范围只能填 公开/私有：" + t);
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    // ---------- 模板 ----------

    /** 写出两页模板：示例数据 + 逐列填写说明。 */
    public void writeTemplate(OutputStream out) {
        ExcelWriter writer = EasyExcel.write(out)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .build();
        try {
            WriteSheet sample = EasyExcel.writerSheet(0, "题库导入模板").head(QuestionImportRow.class).build();
            writer.write(sampleRows(), sample);

            List<List<String>> head = Arrays.asList(
                    Collections.singletonList("列名"), Collections.singletonList("说明"));
            WriteSheet guide = EasyExcel.writerSheet(1, "填写说明").head(head).build();
            writer.write(guideRows(), guide);
        } finally {
            writer.finish();
        }
    }

    private List<QuestionImportRow> sampleRows() {
        List<QuestionImportRow> rows = new ArrayList<>();
        rows.add(sample("单选题", "以下哪项属于 IaaS 服务？",
                new String[]{"阿里云 ECS 云服务器", "钉钉", "Salesforce CRM", "Google Docs"},
                "A", "IaaS 提供虚拟机等基础设施资源", "1", "2"));
        rows.add(sample("多选题", "云计算的部署模型包括？",
                new String[]{"公有云", "私有云", "混合云", "边缘光盘"},
                "ABC", "NIST 定义的部署模型为公有云、私有云、社区云、混合云", "2", "4"));
        rows.add(sample("判断题", "弹性伸缩可以根据负载自动增加或减少计算资源。",
                new String[0], "对", "弹性伸缩按监控指标自动扩缩容", "1", "2"));
        rows.add(sample("填空题", "虚拟化技术中负责管理多个虚拟机的软件层称为____。",
                new String[0], "Hypervisor|虚拟机监控器", "多个可接受答案用 | 分隔", "2", "2"));
        rows.add(sample("简答题", "请对比容器与虚拟机在隔离性、启动速度和资源开销上的差异。",
                new String[0],
                "1. 隔离性：虚拟机基于硬件虚拟化，隔离更强；容器共享宿主内核，隔离较弱。"
                        + "2. 启动速度：虚拟机需启动完整操作系统，通常分钟级；容器秒级启动。"
                        + "3. 资源开销：虚拟机每个实例运行独立 OS，开销大；容器共享内核，开销小、密度高。",
                "评分要点：三个维度各 1 点，比较方向正确即可", "3", "10"));
        rows.add(sample("关键词解释题", "Serverless", new String[0],
                "无服务器计算：一种云计算执行模型，开发者无需管理服务器，代码按事件触发运行，按调用量计费，典型如函数计算 FaaS。",
                "要点：无需管理服务器、事件驱动、按量计费", "2", "5"));
        return rows;
    }

    private QuestionImportRow sample(String type, String content, String[] options, String answer,
                                     String analysis, String difficulty, String score) {
        QuestionImportRow r = new QuestionImportRow();
        r.setQuestionType(type);
        r.setContent(content);
        r.setOptionA(options.length > 0 ? options[0] : null);
        r.setOptionB(options.length > 1 ? options[1] : null);
        r.setOptionC(options.length > 2 ? options[2] : null);
        r.setOptionD(options.length > 3 ? options[3] : null);
        r.setOptionE(options.length > 4 ? options[4] : null);
        r.setOptionF(options.length > 5 ? options[5] : null);
        r.setCorrectAnswer(answer);
        r.setAnalysis(analysis);
        r.setDifficulty(difficulty);
        r.setDefaultScore(score);
        return r;
    }

    private List<List<String>> guideRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("题型", "必填。可选值：单选题、多选题、判断题、填空题、简答题、关键词解释题（也接受 SINGLE/MULTIPLE/JUDGE/FILL/ESSAY/TERM）"));
        rows.add(Arrays.asList("题干", "必填。填空题用连续下划线 ____ 表示空位"));
        rows.add(Arrays.asList("选项A-选项F", "单选/多选题至少填 2 个选项，按 A、B、C… 顺序填写；判断题可留空，系统自动生成 A=正确、B=错误；填空/简答/关键词解释题留空"));
        rows.add(Arrays.asList("正确答案", "单选题填 1 个字母如 A；多选题填多个字母如 ABC 或 A,B,C；判断题填 对/错（也接受 正确/错误、是/否、T/F、A/B）；填空题必填，多个空用 | 分隔；简答/关键词解释题填参考答案，可留空"));
        rows.add(Arrays.asList("解析", "可选。答案解析或评分要点"));
        rows.add(Arrays.asList("难度", "可选。1=简单、2=中等、3=困难，留空默认 1"));
        rows.add(Arrays.asList("默认分值", "可选。留空按题型默认：单选/判断/填空 2 分，多选 4 分，简答 10 分，关键词解释 5 分"));
        rows.add(Arrays.asList("知识点", "可选。填知识点名称，多个用 | 分隔；同名知识点请写完整路径如 云计算基础/IaaS；留空则使用导入时选择的默认知识点"));
        rows.add(Arrays.asList("可见范围", "可选。公开 或 私有，留空默认公开"));
        return rows;
    }

    @Data
    public static class ImportResult {
        private int total;
        private int success;
        private int failed;
        private List<RowError> errors = new ArrayList<>();

        void fail(int rowNo, String message) {
            failed++;
            RowError error = new RowError();
            error.setRowNo(rowNo);
            error.setMessage(message);
            errors.add(error);
        }
    }

    @Data
    public static class RowError {
        private int rowNo;
        private String message;
    }
}
