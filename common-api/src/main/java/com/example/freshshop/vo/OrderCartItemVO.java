package com.example.freshshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单确认商品项VO")
public class OrderCartItemVO {

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品图片")
    private String goodsImage;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer num;

    @Schema(description = "商品小计")
    private BigDecimal totalPrice;
}