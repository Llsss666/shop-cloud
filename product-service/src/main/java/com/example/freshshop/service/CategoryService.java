package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    Result<List<Category>> treeList();

    // 🔥 新增：递归获取所有子分类ID
    void getChildCategoryIds(Long parentId, List<Long> categoryIds);
}