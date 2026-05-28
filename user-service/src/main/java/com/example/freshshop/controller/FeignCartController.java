package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.dto.CartDTO;
import com.example.freshshop.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feign/cart")
public class FeignCartController {

    private final CartService cartService;

    public FeignCartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 内部Feign接口：获取用户购物车
     * 【修复版本：严格返回 List<CartDTO>】
     */
    @GetMapping("/list")
    public Result<List<CartDTO>> getUserCart(@RequestParam Long userId) {
        // 调用你原来的接口
        Result<?> result = cartService.getCartList(userId);

        // 从你的返回结构里取出 cartList → 完全匹配Feign
        Map<String, Object> data = (Map<String, Object>) result.getData();
        List<CartDTO> cartList = (List<CartDTO>) data.get("cartList");

        return Result.success(cartList);
    }

    /**
     * 清空购物车
     */
    @PostMapping("/clear")
    public Result<Void> clearCart(@RequestParam Long userId) {
        cartService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.freshshop.entity.Cart>()
                .eq(com.example.freshshop.entity.Cart::getUserId, userId));
        return Result.success();
    }
}