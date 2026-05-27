package com.example.freshshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.freshshop.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}