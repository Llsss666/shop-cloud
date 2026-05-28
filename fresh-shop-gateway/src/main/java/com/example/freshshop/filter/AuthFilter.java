package com.example.freshshop.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private final String[] skipUrls = {
            "/api/user/login",
            "/api/user/register",
            "/api/file/upload"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getPath().toString();

        // 放行
        for (String skip : skipUrls) {
            if (url.contains(skip)) {
                return chain.filter(exchange);
            }
        }

        // 只检查是否携带 Token，不解析
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return error(exchange, "请先登录");
        }

        // ✅ 不再校验JWT，直接放行（交给业务服务校验）
        return chain.filter(exchange);
    }

    // 统一返回（手写Map，不依赖任何common类）
    private Mono<Void> error(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            // ✅ 直接用 HashMap，不需要引入任何 Result 类！
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", msg);

            byte[] bytes = new ObjectMapper().writeValueAsBytes(result);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}