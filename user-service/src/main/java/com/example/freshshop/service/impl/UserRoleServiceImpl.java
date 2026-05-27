package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.entity.Role;
import com.example.freshshop.entity.UserRole;
import com.example.freshshop.mapper.UserRoleMapper;
import com.example.freshshop.service.RoleService;
import com.example.freshshop.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    @Autowired
    private RoleService roleService;

    @Override
    public String getRoleKeyByUserId(Long userId) {
        List<UserRole> list = this.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (list == null || list.isEmpty()) {
            return "user";
        }
        Role role = roleService.getById(list.get(0).getRoleId());
        return role == null ? "user" : role.getRoleKey();
    }

    @Override
    public List<Long> getRoleIdListByUserId(Long userId) {
        List<UserRole> list = this.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Long> roleIds = new ArrayList<>();
        for (UserRole ur : list) {
            roleIds.add(ur.getRoleId());
        }
        return roleIds;
    }

    @Override
    public String getUserAllRoleNames(Long userId) {
        List<Long> roleIds = getRoleIdListByUserId(userId);
        if (roleIds.isEmpty()) {
            return "未分配";
        }

        List<String> names = new ArrayList<>();
        for (Long rid : roleIds) {
            Role role = roleService.getById(rid);
            if (role != null) {
                names.add(role.getRoleName());
            }
        }

        return String.join(", ", names);
    }
}