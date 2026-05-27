package com.example.freshshop.filter;

import com.example.freshshop.common.Result;
import com.example.freshshop.utils.JwtUtil;
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

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 放行接口
    private final String[] skipUrls = {
            "/user/login",
            "/user/register",
            "/file/upload"
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

        // 获取 token
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return error(exchange, "请先登录");
        }

        String token = auth.replace("Bearer ", "");

        // ===================== 现在正常使用 =====================
        if (!JwtUtil.isTokenValid(token)) {
            return error(exchange, "登录已过期");
        }

        return chain.filter(exchange);
    }

    // 统一返回错误
    private Mono<Void> error(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            Result<?> result = Result.error(msg);
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