package com.example.freshshop.aop;

import com.example.freshshop.annotation.RateLimit;
import com.example.freshshop.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

@Aspect
@Component
public class RateLimitAspect {

    // 🔥 限流专用：DB3
    @Autowired
    @Qualifier("redisTemplateDb3")
    private StringRedisTemplate redisTemplateDb3;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Lua 固定窗口限流（原子、高性能、可靠）
    private static final String LIMIT_LUA =
            "local key = KEYS[1] " +
                    "local max = tonumber(ARGV[1]) " +
                    "local ttl = tonumber(ARGV[2]) " +
                    "local current = redis.call('get', key) " +
                    "if current and tonumber(current) >= max then " +
                    "   return 0 " +
                    "end " +
                    "redis.call('incr', key) " +
                    "redis.call('expire', key, ttl) " +
                    "return 1";

    // ======================
    // 🔥 所有 Controller 接口全部自动限流 ✅
    // ======================
    @Around("execution(* com.example.freshshop.controller..*.*(..))")
    public Object limit(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        // 获取IP
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String key = "limit:" + ip + ":" + uri;

        // 获取接口注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        RateLimit rateLimit = signature.getMethod().getAnnotation(RateLimit.class);

        // 🔥 限流规则（完全按你要求）
        int max;
        if (rateLimit != null && rateLimit.keyApi()) {
            max = 2;      // 关键接口：2次/秒
        } else {
            max = 10;     // 普通接口：默认10次/秒
        }
        int ttl = 1; // 1秒

        // 执行Lua
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LIMIT_LUA);
        script.setResultType(Long.class);

        List<String> keys = Collections.singletonList(key);
        Long allow = redisTemplateDb3.execute(script, keys, String.valueOf(max), String.valueOf(ttl));

        if (allow == null || allow == 0) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error("请求过于频繁，请稍后再试")));
            return null;
        }

        return point.proceed();
    }
}