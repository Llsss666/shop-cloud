package com.example.freshshop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Goods;
import com.example.freshshop.feign.UserFeignClient;
import com.example.freshshop.service.CategoryService;
import com.example.freshshop.service.GoodsService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/goods")
public class GoodsController {

    private final GoodsService goodsService;
    private final CategoryService categoryService;
    private final UserFeignClient userFeignClient;

    // 构造注入
    public GoodsController(GoodsService goodsService,
                           CategoryService categoryService,
                           UserFeignClient userFeignClient) {
        this.goodsService = goodsService;
        this.categoryService = categoryService;
        this.userFeignClient = userFeignClient;
    }

    @Operation(summary = "商品分页列表", description = "登录即可访问，支持按分类、名称筛选")
    @GetMapping("/list")
    public Result<Page<Goods>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<Long> categoryIds = new ArrayList<>();
        if (categoryId != null) {
            categoryIds.add(categoryId);
            categoryService.getChildCategoryIds(categoryId, categoryIds);
        }

        return goodsService.pageList(page, size, categoryId, name, categoryIds);
    }

    @Operation(summary = "商品详情（带缓存）")
    @GetMapping("/detail/{goodsId}")
    public Result<Goods> detail(
            @PathVariable Long goodsId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return goodsService.getGoodsDetail(goodsId);
    }

    @Operation(summary = "新增商品")
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody Goods goods,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        Result<List<String>> result = userFeignClient.getPermissionKeysByUserId(userId);
        List<String> perms = result.getData();

        if (!perms.contains("goods:add")) {
            return Result.error(403, "无权限");
        }
        goodsService.save(goods);
        return Result.success();
    }

    @Operation(summary = "编辑商品")
    @PostMapping("/update")
    public Result<Void> update(
            @RequestBody Goods goods,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        Result<List<String>> result = userFeignClient.getPermissionKeysByUserId(userId);
        List<String> perms = result.getData();

        if (!perms.contains("goods:edit")) {
            return Result.error(403, "无权限");
        }
        goodsService.updateById(goods);
        return Result.success();
    }

    @Operation(summary = "商品上下架")
    @PutMapping("/status/{id}")
    public Result<Void> status(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        Result<List<String>> result = userFeignClient.getPermissionKeysByUserId(userId);
        List<String> perms = result.getData();

        if (!perms.contains("goods:status")) {
            return Result.error(403, "无权限");
        }
        return goodsService.updateStatus(id, status);
    }
}