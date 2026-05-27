package com.example.freshshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.freshshop.entity.User;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 所有 CRUD 自带！不用写任何方法
}