package com.example.freshshop.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 纯 BCrypt 加密工具类（不依赖 Spring Security）
 */
public class EncryptUtil {

    // 加密密码
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    // 校验密码
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}