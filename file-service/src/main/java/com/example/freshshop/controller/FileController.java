package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "文件上传服务")
@RestController
@RequestMapping("/api/file")
public class FileController {

    // 上传目录（可配置在yml，这里先固定）
    private static final String UPLOAD_DIR = "D:/upload/";

    // ===================== 【唯一接口】通用文件上传 =====================
    // 所有上传：头像、商品图、轮播图 都用这一个！
    @Operation(summary = "通用文件上传", description = "上传文件，返回可访问URL")
    @PostMapping("/upload")
    public Result<String> upload(
            @Parameter(description = "文件")
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + ext;
        File dest = new File(dir, fileName);

        try {
            file.transferTo(dest);
            // 返回可访问URL
            String url = "http://localhost:8080/file/" + fileName;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

}