package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.User;
import com.example.freshshop.entity.UserRole;
import com.example.freshshop.mapper.UserMapper;
import com.example.freshshop.mapper.UserRoleMapper;
import com.example.freshshop.service.UserRoleService;
import com.example.freshshop.service.UserService;
import com.example.freshshop.utils.EncryptUtil;
import com.example.freshshop.utils.JwtUtil;
import com.example.freshshop.utils.StringUtil;
import com.example.freshshop.utils.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final RedisUtil redisUtil;
    private final UserRoleService userRoleService;
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(RedisUtil redisUtil, UserRoleService userRoleService, UserRoleMapper userRoleMapper) {
        this.redisUtil = redisUtil;
        this.userRoleService = userRoleService;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Result<String> register(User user) {
        if (StringUtil.isEmpty(user.getUsername())) {
            return Result.error("用户名不能为空");
        }

        Long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        user.setPassword(EncryptUtil.encode(user.getPassword()));
        user.setStatus(1);
        this.save(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L);
        userRoleService.save(userRole);

        return Result.success("注册成功");
    }

    @Override
    public Result<String> login(String username, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (user == null) {
            return Result.error("用户不存在");
        }

        if (user.getStatus() == 0) {
            return Result.error("账号已禁用");
        }

        if (!EncryptUtil.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        String roleKey = userRoleService.getRoleKeyByUserId(user.getId());
        String token = JwtUtil.createToken(user.getId(), roleKey);

        String redisKey = "login:token:" + token;
        redisUtil.set(0, redisKey, String.valueOf(user.getId()), JwtUtil.EXPIRE_DAYS, TimeUnit.DAYS);

        return Result.success(token);
    }

    @Override
    public Result<String> logout(String token) {
        if (StringUtil.isEmpty(token)) {
            return Result.error("token不能为空");
        }
        String redisKey = "login:token:" + token;
        redisUtil.delete(0, redisKey);
        return Result.success("退出成功");
    }

    @Override
    public Result<Void> updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        this.updateById(user);
        return Result.success();
    }

    @Override
    @Transactional
    public void assignUserRole(Long userId, Long roleId) {
        userRoleMapper.deleteByUserId(userId);
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    @Override
    @Transactional
    public void batchAssignUserRole(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);

        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}