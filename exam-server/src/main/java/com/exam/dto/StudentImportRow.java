package com.exam.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportRow {
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("部门")
    private String department;
    @ExcelProperty("班级")
    private String className;
    @ExcelProperty("手机")
    private String phone;
    @ExcelProperty("邮箱")
    private String email;
}
