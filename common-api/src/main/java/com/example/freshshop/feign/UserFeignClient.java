package com.example.freshshop.feign;

import com.example.freshshop.common.Result;
import com.example.freshshop.dto.CartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    // 获取用户购物车
    @GetMapping("/feign/cart/list")
    Result<List<CartDTO>> getUserCart(@RequestParam("userId") Long userId);

    // 清空购物车
    @PostMapping("/feign/cart/clear")
    Result<Void> clearCart(@RequestParam("userId") Long userId);

    // 使用优惠券
    @PostMapping("/feign/coupon/use")
    Result<Void> useCoupon(@RequestParam("couponId") Long couponId);
}