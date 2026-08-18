package com.exam.config;

import com.exam.entity.Exam;
import com.exam.entity.ExamPaper;
import com.exam.entity.ExamPaperQuestion;
import com.exam.entity.ExamStudent;
import com.exam.entity.KnowledgePoint;
import com.exam.entity.Question;
import com.exam.entity.QuestionCategory;
import com.exam.entity.QuestionKnowledge;
import com.exam.entity.QuestionOption;
import com.exam.entity.Student;
import com.exam.entity.SysUser;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamPaperMapper;
import com.exam.mapper.ExamPaperQuestionMapper;
import com.exam.mapper.ExamStudentMapper;
import com.exam.mapper.KnowledgePointMapper;
import com.exam.mapper.QuestionCategoryMapper;
import com.exam.mapper.QuestionKnowledgeMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionOptionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final ExamMapper examMapper;
    private final ExamStudentMapper examStudentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SysUser admin = user("admin", "admin123", "系统管理员", "ADMIN", now);
        SysUser teacher = user("teacher", "teacher123", "张老师", "TEACHER", now);
        SysUser s1u = user("s2024001", "student123", "李明", "STUDENT", now);
        SysUser s2u = user("s2024002", "student123", "王芳", "STUDENT", now);

        Student s1 = student(s1u.getId(), "s2024001", "李明", now);
        Student s2 = student(s2u.getId(), "s2024002", "王芳", now);

        KnowledgePoint java = kp(0L, "Java基础", 1, teacher.getId(), now);
        KnowledgePoint oop = kp(java.getId(), "面向对象", 1, teacher.getId(), now);
        KnowledgePoint enc = kp(oop.getId(), "封装", 1, teacher.getId(), now);
        KnowledgePoint inh = kp(oop.getId(), "继承", 2, teacher.getId(), now);
        KnowledgePoint poly = kp(oop.getId(), "多态", 3, teacher.getId(), now);
        KnowledgePoint col = kp(java.getId(), "集合", 2, teacher.getId(), now);
        KnowledgePoint list = kp(col.getId(), "List", 1, teacher.getId(), now);
        KnowledgePoint map = kp(col.getId(), "Map", 2, teacher.getId(), now);

        QuestionCategory cat = new QuestionCategory();
        cat.setCategoryName("Java基础");
        cat.setParentId(0L);
        cat.setSortNo(1);
        cat.setCreatedAt(now);
        categoryMapper.insert(cat);

        Question q1 = question(cat.getId(), "SINGLE", "下列关于 Java 多态的描述，正确的是？",
                "子类对象可以赋值给父类引用，运行时调用子类重写方法", "方法重载就是多态", 2, teacher.getId(), now);
        option(q1.getId(), "A", "子类对象可以赋值给父类引用，运行时调用子类重写方法", 1, 1);
        option(q1.getId(), "B", "Java 不支持多态", 0, 2);
        option(q1.getId(), "C", "方法重载就是多态", 0, 3);
        option(q1.getId(), "D", "只有抽象类才能体现多态", 0, 4);
        q1.setCorrectAnswer("A");
        questionMapper.updateById(q1);
        link(q1.getId(), poly.getId());

        Question q2 = question(cat.getId(), "MULTIPLE", "下列属于 List 接口实现的有哪些？",
                "ArrayList 和 LinkedList 都实现了 List", "注意区分 Set 与 List", 2, teacher.getId(), now);
        option(q2.getId(), "A", "ArrayList", 1, 1);
        option(q2.getId(), "B", "LinkedList", 1, 2);
        option(q2.getId(), "C", "HashSet", 0, 3);
        option(q2.getId(), "D", "HashMap", 0, 4);
        q2.setCorrectAnswer("A,B");
        questionMapper.updateById(q2);
        link(q2.getId(), list.getId());

        Question q3 = question(cat.getId(), "JUDGE", "HashMap 允许 key 为 null。",
                "HashMap 允许一个 null key", null, 1, teacher.getId(), now);
        option(q3.getId(), "对", "对", 1, 1);
        option(q3.getId(), "错", "错", 0, 2);
        q3.setCorrectAnswer("对");
        questionMapper.updateById(q3);
        link(q3.getId(), map.getId());

        Question q4 = question(cat.getId(), "FILL", "子类使用 ______ 关键字继承父类。",
                "extends 用于类继承", "extends", 1, teacher.getId(), now);
        q4.setCorrectAnswer("extends");
        questionMapper.updateById(q4);
        link(q4.getId(), inh.getId());

        Question q5 = question(cat.getId(), "ESSAY", "请简述封装的含义，并举一个实际例子。",
                "评分要点：隐藏内部实现、通过方法暴露访问、举例合理。",
                "封装是将数据和对数据的操作组合在一起，对外隐藏内部实现细节。例如把字段设为 private，通过 getter/setter 访问。",
                3, teacher.getId(), now);
        q5.setCorrectAnswer("封装是将数据和对数据的操作组合在一起，对外隐藏内部实现细节。");
        q5.setDefaultScore(new BigDecimal("40"));
        questionMapper.updateById(q5);
        link(q5.getId(), enc.getId());

        ExamPaper paper = new ExamPaper();
        paper.setPaperName("Java 基础测验");
        paper.setTotalScore(new BigDecimal("100"));
        paper.setPassScore(new BigDecimal("60"));
        paper.setQuestionCount(5);
        paper.setStatus(1);
        paper.setCreatedBy(teacher.getId());
        paper.setCreatedAt(now);
        paper.setUpdatedAt(now);
        paperMapper.insert(paper);

        pq(paper.getId(), q1.getId(), "15", 1);
        pq(paper.getId(), q2.getId(), "15", 2);
        pq(paper.getId(), q3.getId(), "10", 3);
        pq(paper.getId(), q4.getId(), "20", 4);
        pq(paper.getId(), q5.getId(), "40", 5);

        Exam exam = new Exam();
        exam.setExamName("Java 基础入学测验");
        exam.setPaperId(paper.getId());
        exam.setStartTime(now.minusHours(1));
        exam.setEndTime(now.plusDays(7));
        exam.setDurationMinutes(60);
        exam.setAllowSubmitMinutes(0);
        exam.setResultVisible(1);
        exam.setAnswerVisible(1);
        exam.setStatus("PUBLISHED");
        exam.setCreatedBy(teacher.getId());
        exam.setCreatedAt(now);
        examMapper.insert(exam);

        assign(exam.getId(), s1.getId());
        assign(exam.getId(), s2.getId());
    }

    private SysUser user(String username, String password, String name, String role, LocalDateTime now) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(name);
        u.setRole(role);
        u.setStatus(1);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        userMapper.insert(u);
        return u;
    }

    private Student student(Long userId, String no, String name, LocalDateTime now) {
        Student s = new Student();
        s.setUserId(userId);
        s.setStudentNo(no);
        s.setName(name);
        s.setDepartment("计算机系");
        s.setClassName("软件2301");
        s.setStatus(1);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        studentMapper.insert(s);
        return s;
    }

    private KnowledgePoint kp(Long parent, String name, int sort, Long by, LocalDateTime now) {
        KnowledgePoint p = new KnowledgePoint();
        p.setParentId(parent);
        p.setName(name);
        p.setSortNo(sort);
        p.setStatus(1);
        p.setCreatedBy(by);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        knowledgePointMapper.insert(p);
        return p;
    }

    private Question question(Long cat, String type, String content, String analysis, String correct,
                              int difficulty, Long by, LocalDateTime now) {
        Question q = new Question();
        q.setCategoryId(cat);
        q.setQuestionType(type);
        q.setContent(content);
        q.setAnalysis(analysis);
        q.setCorrectAnswer(correct);
        q.setDifficulty(difficulty);
        q.setDefaultScore(BigDecimal.TEN);
        q.setVisibility("PUBLIC");
        q.setStatus(1);
        q.setCreatedBy(by);
        q.setCreatedAt(now);
        q.setUpdatedAt(now);
        questionMapper.insert(q);
        return q;
    }

    private void option(Long qid, String key, String content, int correct, int sort) {
        QuestionOption o = new QuestionOption();
        o.setQuestionId(qid);
        o.setOptionKey(key);
        o.setOptionContent(content);
        o.setIsCorrect(correct);
        o.setSortNo(sort);
        optionMapper.insert(o);
    }

    private void link(Long qid, Long kpid) {
        QuestionKnowledge k = new QuestionKnowledge();
        k.setQuestionId(qid);
        k.setKnowledgePointId(kpid);
        k.setWeight(BigDecimal.ONE);
        questionKnowledgeMapper.insert(k);
    }

    private void pq(Long paperId, Long qid, String score, int sort) {
        ExamPaperQuestion row = new ExamPaperQuestion();
        row.setPaperId(paperId);
        row.setQuestionId(qid);
        row.setQuestionScore(new BigDecimal(score));
        row.setSortNo(sort);
        paperQuestionMapper.insert(row);
    }

    private void assign(Long examId, Long studentId) {
        ExamStudent es = new ExamStudent();
        es.setExamId(examId);
        es.setStudentId(studentId);
        es.setExamStatus("NOT_STARTED");
        examStudentMapper.insert(es);
    }
}
