package com.example.freshshop.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 */
public class DateUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 当前时间格式化
    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    // 时间转字符串
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(FORMATTER);
    }

    // 字符串转时间
    public static LocalDateTime parse(String str) {
        return LocalDateTime.parse(str, FORMATTER);
    }

    // 获取当前时间戳（秒）
    public static long currentSecond() {
        return System.currentTimeMillis() / 1000;
    }
}