package com.example.freshshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 全局配置：Long / Long 包装类 序列化为字符串，解决JS大数精度丢失
        objectMapper.registerModule(new JavaTimeModule())
                .registerModule(
                        new com.fasterxml.jackson.databind.module.SimpleModule()
                                // Long 转字符串
                                .addSerializer(Long.class, ToStringSerializer.instance)
                                .addSerializer(Long.TYPE, ToStringSerializer.instance)
                );

        // 可选：LocalDateTime 时间格式化（按需保留）
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        String pattern = "yyyy-MM-dd HH:mm:ss";
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(pattern)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(pattern)));
        objectMapper.registerModule(javaTimeModule);

        return objectMapper;
    }
}
