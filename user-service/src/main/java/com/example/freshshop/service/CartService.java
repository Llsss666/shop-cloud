package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Cart;

public interface CartService extends IService<Cart> {
    Result<?> getCartList(Long userId);
    Result<?> addCart(Long userId, Long goodsId, Integer num);
    Result<?> deleteCart(Long id);
}