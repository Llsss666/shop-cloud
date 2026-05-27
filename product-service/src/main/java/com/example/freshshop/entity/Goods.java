package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goods")
@Schema(description = "商品实体")
public class Goods {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "库存数量")
    private Integer stock;

    @Schema(description = "商品规格")
    private String spec;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "状态 0-下架 1-上架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}