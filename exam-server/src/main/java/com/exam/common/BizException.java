package com.exam.common;

import lombok.Getter;

/**
 * 业务失败时抛出，例如「考试已结束」「请勿重复交卷」。
 * 由 GlobalExceptionHandler 转成 Result，不用在每个接口里 try-catch。
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
