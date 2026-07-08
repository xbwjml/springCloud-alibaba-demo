package com.demo.common.result;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一接口响应结构。
 */
@Getter
@Setter
@NoArgsConstructor
public class Result<T> implements Serializable {

    private static final int SUCCESS_CODE = 0;
    private static final int ERROR_CODE = 500;

    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, "success", null);
    }

    public static Result<Void> fail(String message) {
        return fail(ERROR_CODE, message);
    }

    public static Result<Void> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return code == SUCCESS_CODE;
    }
}
