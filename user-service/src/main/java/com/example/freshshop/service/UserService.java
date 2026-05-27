package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.User;
import com.example.freshshop.vo.OrderConfirmVO;

import java.util.List;

public interface UserService extends IService<User> {
    Result<String> register(User user);
    Result<String> login(String username, String password);
    Result<Void> updateStatus(Long id, Integer status);
    void assignUserRole(Long userId, Long roleId);
    Result<String> logout(String token);
    // 新增
    void batchAssignUserRole(Long userId, List<Long> roleIds);
}