package com.demo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 可预期的业务异常，由全局异常处理器转换为统一响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus httpStatus;
    private final int code;

    public BusinessException(int code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }

    public BusinessException(HttpStatus httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
