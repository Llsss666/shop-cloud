package com.example.freshshop.aop;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
public class WebLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(WebLogAspect.class);

    // 匹配你所有 controller 接口
    @Pointcut("execution(* com.example.freshshop.controller..*.*(..))")
    public void webLog() {}

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 执行目标接口
        Object result = joinPoint.proceed();

        logger.info("======================= 接口请求开始 =======================");
        logger.info("URL: {} {}", request.getMethod(), request.getRequestURL());
        logger.info("IP: {}", request.getRemoteAddr());
        logger.info("方法: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
        logger.info("参数: {}", Arrays.toString(joinPoint.getArgs()));
        logger.info("返回: {}", JSON.toJSONString(result));
        logger.info("耗时: {} ms", System.currentTimeMillis() - startTime);
        logger.info("======================= 接口请求结束 =======================");

        return result;
    }
}