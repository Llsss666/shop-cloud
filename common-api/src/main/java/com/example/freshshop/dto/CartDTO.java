package com.example.freshshop.dto;

import lombok.Data;

/**
 * 给订单服务使用的购物车DTO
 */
@Data
public class CartDTO {
    private Long id;           // 购物车ID
    private Long userId;       // 用户ID
    private Long goodsId;      // 商品ID
    private Integer num;       // 购买数量
}