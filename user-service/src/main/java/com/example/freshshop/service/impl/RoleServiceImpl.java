package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Role;
import com.example.freshshop.entity.RolePermission;
import com.example.freshshop.mapper.RoleMapper;
import com.example.freshshop.mapper.RolePermissionMapper;
import com.example.freshshop.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional
    public Result<Void> assignPermission(Long roleId, Long[] permissionIds) {
        // 先删除旧权限
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        // 新增新权限
        Arrays.stream(permissionIds).forEach(pid -> {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        });
        return Result.success();
    }
}