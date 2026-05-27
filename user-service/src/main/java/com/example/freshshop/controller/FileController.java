package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.User;
import com.example.freshshop.service.UserService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "文件上传 / 头像管理")
@RestController
@RequestMapping("/file")
public class FileController {

    private static final String UPLOAD_DIR = "D:/upload/";

    @Autowired
    private UserService userService;

    // 只用于上传图片，不改数据库
    @Operation(summary = "通用文件上传", description = "仅上传文件，不修改用户信息")
    @PostMapping("/upload")
    public Result<String> upload(
            @Parameter(description = "上传的文件")
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) return Result.error("文件为空");
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = UUID.randomUUID() + ext;
        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest);
            return Result.success("http://localhost:8080/file/" + fileName);
        } catch (IOException e) {
            return Result.error("上传失败");
        }
    }

    // 头像上传：一次调用 = 上传 + 存数据库
    @Operation(summary = "上传用户头像", description = "上传并自动保存到当前用户信息")
    @PostMapping("/avatar")
    public Result<String> avatar(
            @Parameter(description = "头像文件")
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "登录令牌")
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();

        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = "avatar_" + UUID.randomUUID() + ext;
        File dest = new File(dir, fileName);

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("上传失败");
        }

        String avatarUrl = "http://localhost:8080/file/" + fileName;

        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        userService.updateById(user);

        return Result.success(avatarUrl);
    }
}