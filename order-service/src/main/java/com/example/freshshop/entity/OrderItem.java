package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item")
@Schema(description = "订单明细实体")
public class OrderItem {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "购买数量")
    private Integer num;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品图片")
    private String goodsImage;
}