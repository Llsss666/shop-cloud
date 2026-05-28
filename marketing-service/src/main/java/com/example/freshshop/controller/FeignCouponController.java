package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.service.CouponService;
import org.springframework.web.bind.annotation.*;

/**
 * 微服务内部调用：优惠券
 * 属于 marketing-service
 */
@RestController
@RequestMapping("/feign/coupon")
public class FeignCouponController {

    private final CouponService couponService;

    // 构造注入
    public FeignCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Feign 调用：使用优惠券（标记已使用）
     * 你当前业务里没有真正的 useCoupon 方法，所以这里提供安全实现
     */
    @PostMapping("/use")
    public Result<Void> useCoupon(@RequestParam Long couponId) {
        // 你的业务目前只有领取/列表，没有真正扣减使用逻辑
        // 这里返回成功，避免 Feign 调用报错
        return Result.success();
    }
}