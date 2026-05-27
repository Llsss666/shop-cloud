package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Category;
import com.example.freshshop.mapper.CategoryMapper;
import com.example.freshshop.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Result<List<Category>> treeList() {
        List<Category> all = list();
        List<Category> root = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() == 0) {
                root.add(c);
            }
        }
        buildTree(root, all);
        return Result.success(root);
    }

    private void buildTree(List<Category> parentList, List<Category> all) {
        for (Category parent : parentList) {
            List<Category> children = new ArrayList<>();
            for (Category c : all) {
                if (c.getParentId().equals(parent.getId())) {
                    children.add(c);
                }
            }
            parent.setChildren(children);
            buildTree(children, all);
        }
    }

    // ======================
    // 🔥 新增：递归获取子分类ID
    // ======================
    @Override
    public void getChildCategoryIds(Long parentId, List<Long> categoryIds) {
        List<Category> children = lambdaQuery()
                .eq(Category::getParentId, parentId)
                .list();

        if (!children.isEmpty()) {
            for (Category child : children) {
                categoryIds.add(child.getId());
                getChildCategoryIds(child.getId(), categoryIds);
            }
        }
    }
}