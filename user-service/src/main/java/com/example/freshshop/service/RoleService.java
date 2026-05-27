package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Role;

public interface RoleService extends IService<Role> {
    Result<Void> assignPermission(Long roleId, Long[] permissionIds);
}