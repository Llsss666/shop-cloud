package com.example.freshshop.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 给订单服务使用的商品DTO
 * 只保留订单需要的字段
 */
@Data
public class GoodsDTO {
    private Long id;           // 商品ID
    private String name;       // 商品名称
    private String image;      // 商品图片
    private BigDecimal price;  // 商品单价
    private Integer stock;     // 库存
}