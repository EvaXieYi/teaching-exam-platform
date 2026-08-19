/**
 * 对外 HTTP 接口（网址清单）。
 * 这里只收参数、调 Service、返回 Result，不写评分、组卷等业务细节。
 * <ul>
 *   <li>AuthController：登录</li>
 *   <li>AccountController：教师账号 / 学生</li>
 *   <li>CatalogController：知识点、题库分类</li>
 *   <li>QuestionController / PaperController / ExamManageController：题、卷、考试</li>
 *   <li>StudentExamController：学生答题（路径以 /api/student 开头）</li>
 *   <li>MarkingAnalysisController：阅卷、成绩、学情</li>
 * </ul>
 */
package com.exam.controller;
