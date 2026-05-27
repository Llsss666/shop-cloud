package com.example.freshshop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Seckill;
import com.example.freshshop.service.SeckillService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "管理端 - 秒杀活动管理")
@RestController
@RequestMapping("/admin/seckill")
public class AdminSeckillController {

    private final SeckillService seckillService;

    // 🔥 只注入 service，完全不操作 Redis
    public AdminSeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    private Result<Void> checkAdmin(String token) {
        try {
            String realToken = token.replace("Bearer ", "");
            String roleKey = JwtUtil.getRoleKeyFromToken(realToken);
            if ("admin".equals(roleKey)) {
                return null;
            }
            return Result.error("无权限，仅管理员可操作");
        } catch (Exception e) {
            return Result.error("token无效");
        }
    }

    // 只保存数据库 ✅
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody Seckill seckill,
            @RequestHeader("Authorization") String token) {
        Result<Void> check = checkAdmin(token);
        if (check != null) return check;
        seckill.setCreateTime(LocalDateTime.now());
        seckillService.save(seckill);
        return Result.success();
    }

    // 只更新数据库 ✅
    @PutMapping("/update")
    public Result<Void> update(
            @RequestBody Seckill seckill,
            @RequestHeader("Authorization") String token) {
        Result<Void> check = checkAdmin(token);
        if (check != null) return check;
        seckillService.updateById(seckill);
        return Result.success();
    }

    // 只删除数据库 ✅
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Result<Void> check = checkAdmin(token);
        if (check != null) return check;
        seckillService.removeById(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<Page<Seckill>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader("Authorization") String token) {
        Result<Void> check = checkAdmin(token);
        if (check != null) return Result.error(check.getMsg());
        Page<Seckill> pageParam = new Page<>(page, size);
        return Result.success(seckillService.page(pageParam));
    }
}