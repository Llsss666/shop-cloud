package com.example.freshshop.controller;

import com.example.freshshop.annotation.RateLimit;
import com.example.freshshop.common.Result;
import com.example.freshshop.service.SeckillService;
import com.example.freshshop.utils.JwtUtil;
import com.example.freshshop.vo.SeckillVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    // 注入我们的秒杀服务
    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    // ==============================
    // 列表：使用我们预热 Redis 的接口
    // ==============================
    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public Result<List<SeckillVO>> list(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        // 调用我们的 Redis 预热 + 查询接口
        return seckillService.listNow();
    }

    // ==============================
    // 【真正秒杀】Redis + 分布式锁 + MQ
    // ==============================
    @Operation(summary = "秒杀参与（安全高并发版）")
    @RateLimit(keyApi = true)
    @PostMapping("/buy/{id}")
    public Result<Void> buy(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        // 调用安全秒杀
        return seckillService.doSeckill(id, userId);
    }
}