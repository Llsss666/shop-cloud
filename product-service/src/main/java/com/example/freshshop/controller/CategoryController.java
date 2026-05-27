package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Category;
import com.example.freshshop.feign.UserFeignClient;
import com.example.freshshop.service.CategoryService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品分类管理")
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserFeignClient userFeignClient;

    // 构造注入
    public CategoryController(CategoryService categoryService, UserFeignClient userFeignClient) {
        this.categoryService = categoryService;
        this.userFeignClient = userFeignClient;
    }

    @Operation(summary = "获取分类树形结构", description = "登录即可访问")
    @GetMapping("/tree")
    public Result<List<Category>> tree(
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return categoryService.treeList();
    }

    @Operation(summary = "新增/修改商品分类", description = "仅管理员")
    @PostMapping
    public Result<Void> save(
            @RequestBody Category category,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) return Result.error(401, "请登录");

        // Feign 远程调用获取权限
        Result<List<String>> result = userFeignClient.getPermissionKeysByUserId(userId);
        List<String> perms = result.getData();

        if (!perms.contains("category:add") && !perms.contains("category:edit")) {
            return Result.error(403, "无权限");
        }

        categoryService.saveOrUpdate(category);
        return Result.success();
    }

    @Operation(summary = "删除分类", description = "仅管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) return Result.error(401, "请登录");

        Result<List<String>> result = userFeignClient.getPermissionKeysByUserId(userId);
        List<String> perms = result.getData();

        if (!perms.contains("category:del")) {
            return Result.error(403, "无权限");
        }

        categoryService.removeById(id);
        return Result.success();
    }
}