-- MySQL 8.4 建库建表（考试平台）
CREATE DATABASE IF NOT EXISTS exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
);

CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    student_no VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    class_name VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(100),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_student_user_id (user_id),
    KEY idx_student_department (department)
);

CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    sort_no INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_kp_parent (parent_id)
);

CREATE TABLE IF NOT EXISTS question_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_no INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    correct_answer TEXT,
    analysis TEXT,
    difficulty TINYINT DEFAULT 1,
    default_score DECIMAL(6,2) DEFAULT 1.00,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_question_category (category_id),
    KEY idx_question_type (question_type)
);

CREATE TABLE IF NOT EXISTS question_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(10) NOT NULL,
    option_content TEXT NOT NULL,
    is_correct TINYINT NOT NULL DEFAULT 0,
    sort_no INT DEFAULT 0,
    KEY idx_option_question_id (question_id)
);

CREATE TABLE IF NOT EXISTS question_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    weight DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    UNIQUE KEY uk_question_kp (question_id, knowledge_point_id),
    KEY idx_qk_kp (knowledge_point_id)
);

CREATE TABLE IF NOT EXISTS exam_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_name VARCHAR(200) NOT NULL,
    total_score DECIMAL(8,2) NOT NULL,
    pass_score DECIMAL(8,2) NOT NULL,
    question_count INT NOT NULL,
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_paper_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_score DECIMAL(6,2) NOT NULL,
    sort_no INT NOT NULL,
    UNIQUE KEY uk_paper_question (paper_id, question_id),
    KEY idx_paper_id (paper_id)
);

CREATE TABLE IF NOT EXISTS exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_name VARCHAR(200) NOT NULL,
    paper_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    allow_submit_minutes INT DEFAULT 0,
    result_visible TINYINT DEFAULT 1,
    answer_visible TINYINT DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_exam_time (start_time, end_time)
);

CREATE TABLE IF NOT EXISTS exam_student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    exam_status VARCHAR(20) DEFAULT 'NOT_STARTED',
    UNIQUE KEY uk_exam_student (exam_id, student_id)
);

CREATE TABLE IF NOT EXISTS exam_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    start_time DATETIME,
    submit_time DATETIME,
    objective_score DECIMAL(8,2) DEFAULT 0,
    subjective_score DECIMAL(8,2) DEFAULT 0,
    total_score DECIMAL(8,2) DEFAULT 0,
    passed TINYINT DEFAULT 0,
    submit_type VARCHAR(20),
    record_status VARCHAR(20),
    UNIQUE KEY uk_exam_record (exam_id, student_id),
    KEY idx_record_student (student_id),
    KEY idx_record_exam_score (exam_id, total_score)
);

CREATE TABLE IF NOT EXISTS exam_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    student_answer TEXT,
    correct_answer_snapshot TEXT,
    question_content_snapshot TEXT,
    question_type_snapshot VARCHAR(20),
    question_score DECIMAL(6,2) DEFAULT 0,
    score DECIMAL(6,2) DEFAULT 0,
    is_correct TINYINT,
    flagged TINYINT DEFAULT 0,
    comment TEXT,
    marked_by BIGINT,
    marked_at DATETIME,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_question (record_id, question_id),
    KEY idx_answer_record (record_id)
);

CREATE TABLE IF NOT EXISTS student_knowledge_stat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    exam_count INT NOT NULL DEFAULT 0,
    question_count INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    got_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    full_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    mastery_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    last_exam_id BIGINT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_kp (student_id, knowledge_point_id),
    KEY idx_sks_kp_rate (knowledge_point_id, mastery_rate)
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(100),
    detail TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
