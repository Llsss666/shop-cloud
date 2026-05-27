package com.example.freshshop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.freshshop.annotation.RateLimit;
import com.example.freshshop.common.Result;
import com.example.freshshop.dto.AvatarDTO;
import com.example.freshshop.dto.UserLoginDTO;
import com.example.freshshop.dto.UserRoleBatchDTO;
import com.example.freshshop.dto.UserRoleDTO;
import com.example.freshshop.entity.User;
import com.example.freshshop.service.PermissionService;
import com.example.freshshop.service.UserRoleService;
import com.example.freshshop.service.UserService;
import com.example.freshshop.utils.JwtUtil;
import com.example.freshshop.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserRoleService userRoleService;

    // 注册
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        return userService.register(user);
    }

    // 登录
    @Operation(summary = "用户登录")
    @RateLimit(keyApi = true)
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginDTO dto) {
        return userService.login(dto.getUsername(), dto.getPassword());
    }
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String token) {
        // 👇 加这行日志，只要进入接口就会打印！
        String realToken = token.replace("Bearer ", "");
        return userService.logout(realToken);
    }
    // 获取个人信息
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> info(@RequestHeader("Authorization") String token) {
        String realToken = token.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(realToken);

        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);
        if (!permissions.contains("user:info")) {
            return Result.error(403, "无权限访问");
        }

        User user = userService.getById(userId);
        user.setPassword(null);
        return Result.success(user);
    }
    @Operation(summary = "修改用户头像")
    @PostMapping("/avatar/update")
    public Result<String> updateAvatar(
            @RequestBody AvatarDTO dto,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        User user = new User();
        user.setId(userId);
        user.setAvatar(dto.getAvatarUrl());
        userService.updateById(user);
        return Result.success("修改成功");
    }
    // 修改个人信息
    @Operation(summary = "修改个人信息")
    @PostMapping("/update")
    public Result<User> updateInfo(@RequestBody User user, @RequestHeader("Authorization") String token) {
        String realToken = token.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(realToken);

        if (!userId.equals(user.getId())) {
            return Result.error(403, "无权限修改");
        }

        userService.updateById(user);
        return Result.success(user);
    }

    // ===================== ✅ 用户列表（最终正确版） =====================
    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    public Result<Page<UserVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader("Authorization") String token
    ) {
        Page<User> userPage = userService.page(new Page<>(page, size));
        Page<UserVO> voPage = new Page<>();
        voPage.setCurrent(userPage.getCurrent());
        voPage.setSize(userPage.getSize());
        voPage.setTotal(userPage.getTotal());
        voPage.setPages(userPage.getPages());

        List<UserVO> voList = userPage.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
            vo.setStatus(user.getStatus());

            try {
                // 多角色名称，用逗号分隔
                vo.setRoleName(userRoleService.getUserAllRoleNames(user.getId()));
            } catch (Exception e) {
                vo.setRoleName("未分配");
            }

            return vo;
        }).toList();

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    // 修改用户状态
    @Operation(summary = "修改用户状态")
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestHeader("Authorization") String token
    ) {
        String realToken = token.replace("Bearer ", "");
        String roleKey = JwtUtil.getRoleKeyFromToken(realToken);

        if ("admin".equals(roleKey)) {
            return userService.updateStatus(id, status);
        }

        Long userId = JwtUtil.getUserIdFromToken(realToken);
        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);
        if (!permissions.contains("user:status")) {
            return Result.error(403, "无权限");
        }

        return userService.updateStatus(id, status);
    }

    // 分配单个角色
    @Operation(summary = "分配单个角色")
    @PostMapping("/assign/role")
    public Result assignRole(
            @RequestBody UserRoleDTO dto,
            @RequestHeader("Authorization") String token
    ) {
        String roleKey = JwtUtil.getRoleKeyFromToken(token.replace("Bearer ", ""));
        if (!"admin".equals(roleKey)) {
            return Result.error(403, "无权限");
        }

        userService.assignUserRole(dto.getUserId(), dto.getRoleId());
        return Result.success();
    }

    // 批量分配多角色
    @Operation(summary = "批量分配多个角色")
    @PostMapping("/batchAssignRole")
    public Result batchAssignRole(
            @RequestBody UserRoleBatchDTO dto,
            @RequestHeader("Authorization") String token
    ) {
        String roleKey = JwtUtil.getRoleKeyFromToken(token.replace("Bearer ", ""));
        if (!"admin".equals(roleKey)) {
            return Result.error(403, "无权限");
        }

        userService.batchAssignUserRole(dto.getUserId(), dto.getRoleIds());
        return Result.success();
    }
}