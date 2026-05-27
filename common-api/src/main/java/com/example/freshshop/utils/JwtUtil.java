package com.example.freshshop.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "your-secret-key-32bytes-long-secure";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    // 过期时间 7天（与Redis同步）
    public static final long EXPIRE_DAYS = 7;
    private static final long EXPIRE_MILLIS = 1000L * 60 * 60 * 24 * EXPIRE_DAYS;

    // 创建 token
    public static String createToken(Long userId, String roleKey) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("roleKey", roleKey)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_MILLIS))
                .signWith(KEY)
                .compact();
    }

    // 解析 token
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 获取角色
    public static String getRoleKeyFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roleKey", String.class);
    }

    // 获取用户ID
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
}