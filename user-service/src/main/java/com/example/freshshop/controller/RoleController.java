package com.example.freshshop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Role;
import com.example.freshshop.service.PermissionService;
import com.example.freshshop.service.RoleService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Operation(summary = "新增/修改角色", description = "需要 role:add 或 role:edit 权限")
    @PostMapping
    public Result<Void> save(
            @Parameter(description = "角色实体信息")
            @RequestBody Role role,

            @Parameter(description = "登录令牌")
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);

        if (!permissions.contains("role:add") && !permissions.contains("role:edit")) {
            return Result.error(403, "无权限操作");
        }

        roleService.saveOrUpdate(role);
        return Result.success();
    }

    @Operation(summary = "角色分页列表", description = "需要 role:list 权限")
    @GetMapping("/list")
    public Result<Page<Role>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);

        if (!permissions.contains("role:list")) {
            return Result.error(403, "无权限查看");
        }

        return Result.success(roleService.page(new Page<>(page, size)));
    }

    @Operation(summary = "角色分配权限", description = "需要 role:assign 权限")
    @PostMapping("/assign")
    public Result<Void> assignPermission(
            @RequestParam Long roleId,
            @RequestParam Long[] permissionIds,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);

        if (!permissions.contains("role:assign")) {
            return Result.error(403, "无权限分配权限");
        }

        return roleService.assignPermission(roleId, permissionIds);
    }

    // ===================== ✅【新增】删除角色（补全接口） =====================
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);
        if (!permissions.contains("role:delete")) {
            return Result.error(403, "无权限删除");
        }

        roleService.removeById(id);
        return Result.success();
    }
}