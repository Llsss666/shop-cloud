package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@TableName("category")
@Schema(description = "商品分类实体")
public class Category {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图片")
    private String image;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态 0-禁用 1-启用")
    private Integer status;

    @Schema(description = "子分类列表")
    @TableField(exist = false)
    private List<Category> children;
}