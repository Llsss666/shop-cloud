package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Permission;
import com.example.freshshop.service.PermissionService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    // ===================== 【修复】给前端 el-tree 用的权限树（带勾选） =====================
    @Operation(summary = "获取权限树形结构（带角色选中状态）", description = "管理员专用")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree(
            @RequestParam(required = false) Long roleId,
            @Parameter(description = "登录令牌") @RequestHeader("Authorization") String token
    ) {
        return Result.success(permissionService.getPermissionTreeWithChecked(roleId));
    }

    // ===================== 【原有】保留不动 =====================
    @Operation(summary = "获取权限树形结构", description = "管理员专用")
    @GetMapping("/treeList")
    public Result<List<Permission>> treeList() {
        return permissionService.treeList();
    }

    @Operation(summary = "新增/修改权限", description = "仅管理员可调用")
    @PostMapping
    public Result<Void> save(
            @Parameter(description = "权限实体")
            @RequestBody Permission permission,
            @RequestHeader("Authorization") String token
    ) {
        // 简单权限校验
        String roleKey = JwtUtil.getRoleKeyFromToken(token.replace("Bearer ", ""));
        if (!"admin".equals(roleKey)) {
            return Result.error(403, "无权限");
        }

        permissionService.saveOrUpdate(permission);
        return Result.success();
    }
}