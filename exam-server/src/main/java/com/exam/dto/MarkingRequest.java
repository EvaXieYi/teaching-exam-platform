package com.exam.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarkingRequest {
    private BigDecimal score;
    private String comment;
}
