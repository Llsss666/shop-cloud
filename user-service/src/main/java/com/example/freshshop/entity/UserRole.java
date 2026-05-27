package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("user_role")
@Schema(description = "用户角色关联实体")
public class UserRole {

    // 🔥 只加这两行（主键自增）

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID")
    private Long roleId;
}