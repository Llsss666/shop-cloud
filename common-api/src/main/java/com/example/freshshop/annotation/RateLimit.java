package com.example.freshshop.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    // 每秒限制次数
    int limit() default 10;

    // 关键接口：2次/秒
    boolean keyApi() default false;
}