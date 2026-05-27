package com.example.freshshop.utils;

/**
 * 字符串工具类
 */
public class StringUtil {

    // 判空
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    // 判非空
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    // 字符串拼接
    public static String concat(String... strs) {
        StringBuilder sb = new StringBuilder();
        for (String s : strs) {
            if (isNotEmpty(s)) {
                sb.append(s);
            }
        }
        return sb.toString();
    }
}