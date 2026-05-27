package com.example.freshshop.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    // 状态码：200成功，500失败
    private Integer code;

    // 提示信息
    private String msg;

    // 返回数据
    private T data;

    // 成功（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 成功（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 失败
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    // 失败（自定义状态码）
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}