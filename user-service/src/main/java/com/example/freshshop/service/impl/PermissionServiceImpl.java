package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Permission;
import com.example.freshshop.entity.RolePermission;
import com.example.freshshop.entity.UserRole;
import com.example.freshshop.mapper.PermissionMapper;
import com.example.freshshop.mapper.RolePermissionMapper;
import com.example.freshshop.mapper.UserRoleMapper;
import com.example.freshshop.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    // 原有树形结构（不动）
    @Override
    public Result<List<Permission>> treeList() {
        List<Permission> all = baseMapper.selectList(null);
        List<Permission> root = new ArrayList<>();
        for (Permission p : all) {
            if (p.getParentId() == 0) {
                root.add(p);
            }
        }
        buildTree(root, all);
        return Result.success(root);
    }

    private void buildTree(List<Permission> parentList, List<Permission> all) {
        for (Permission parent : parentList) {
            List<Permission> children = new ArrayList<>();
            for (Permission p : all) {
                if (p.getParentId().equals(parent.getId())) {
                    children.add(p);
                }
            }
            parent.setChildren(children);
            buildTree(children, all);
        }
    }

    // ====================== 原有权限查询（不动） ======================
    @Override
    public List<String> getPermissionKeysByUserId(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
        );

        if (userRoles.isEmpty()) return new ArrayList<>();

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, roleIds)
        );

        if (rolePermissions.isEmpty()) return new ArrayList<>();

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        List<Permission> permissions = baseMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permissionIds)
        );

        return permissions.stream()
                .map(Permission::getPermissionKey)
                .collect(Collectors.toList());
    }

    // ====================== ✅ 新增：给前端 el-tree 用的权限树（带勾选） ======================
    public List<Map<String, Object>> getPermissionTreeWithChecked(Long roleId) {
        // 1. 获取全量权限树
        List<Permission> allPermissions = baseMapper.selectList(null);
        List<Permission> rootList = new ArrayList<>();
        for (Permission p : allPermissions) {
            if (p.getParentId() == 0) {
                rootList.add(p);
            }
        }
        buildTree(rootList, allPermissions);

        // 2. 获取当前角色已选中的权限ID
        Set<Long> checkedIds = new HashSet<>();
        if (roleId != null) {
            List<RolePermission> rps = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, roleId)
            );
            checkedIds = rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
        }

        // 3. 转换成 el-tree 能识别的结构
        return convertToTreeMap(rootList, checkedIds);
    }

    // 转换为 el-tree 所需格式
    private List<Map<String, Object>> convertToTreeMap(List<Permission> treeNodes, Set<Long> checkedIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Permission p : treeNodes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("label", p.getName());
            map.put("disabled", false);

            if (checkedIds.contains(p.getId())) {
                map.put("checked", true);
            }

            if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                map.put("children", convertToTreeMap(p.getChildren(), checkedIds));
            } else {
                map.put("children", new ArrayList<>());
            }
            list.add(map);
        }
        return list;
    }
}