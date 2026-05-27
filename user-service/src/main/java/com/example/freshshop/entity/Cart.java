package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cart")
@Schema(description = "购物车实体")
public class Cart {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品数量")
    private Integer num;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}