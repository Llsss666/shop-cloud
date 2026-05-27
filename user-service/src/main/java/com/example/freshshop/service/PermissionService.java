package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Permission;

import java.util.List;
import java.util.Map;

public interface PermissionService extends IService<Permission> {
    Result<List<Permission>> treeList();
    List<String> getPermissionKeysByUserId(Long userId); // 加这个
    List<Map<String, Object>> getPermissionTreeWithChecked(Long roleId);
}