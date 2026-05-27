package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.entity.UserRole;
import java.util.List;

public interface UserRoleService extends IService<UserRole> {

    // 根据用户ID查角色标识（登录用）
    String getRoleKeyByUserId(Long userId);

    // 获取用户所有角色ID（支持多角色）
    List<Long> getRoleIdListByUserId(Long userId);

    // 获取角色名称，多个用逗号分隔
    String getUserAllRoleNames(Long userId);
}