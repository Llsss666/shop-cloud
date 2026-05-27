package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feign/permission")
public class UserPermissionFeignController {

    private final PermissionService permissionService;

    public UserPermissionFeignController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/getPermissionKeysByUserId")
    public Result<List<String>> getPermissionKeysByUserId(@RequestParam Long userId) {
        List<String> list = permissionService.getPermissionKeysByUserId(userId);
        return Result.success(list);
    }
}