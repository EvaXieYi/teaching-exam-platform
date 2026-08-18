package com.exam.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> records;

    public static <T> PageResult<T> of(long total, List<T> records) {
        PageResult<T> p = new PageResult<>();
        p.setTotal(total);
        p.setRecords(records);
        return p;
    }
}
