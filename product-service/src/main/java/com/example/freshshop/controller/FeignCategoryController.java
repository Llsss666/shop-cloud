package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Category;
import com.example.freshshop.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/feign/category")
public class FeignCategoryController {

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping("/getName")
    public Result<String> getCategoryName(@RequestParam Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        String name = category != null ? category.getName() : "未知分类";
        return Result.success(name);
    }
}