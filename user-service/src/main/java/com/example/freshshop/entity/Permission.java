package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@TableName("permission")
@Schema(description = "权限实体")
public class Permission {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父权限ID")
    private Long parentId;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限标识")
    private String permissionKey;

    @Schema(description = "类型 1-菜单 2-按钮")
    private Integer type;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "子权限列表")
    @TableField(exist = false)
    private List<Permission> children;
}