package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.dto.CouponDTO;
import com.example.freshshop.entity.Coupon;
import com.example.freshshop.entity.UserCoupon;
import com.example.freshshop.mapper.CouponMapper;
import com.example.freshshop.mapper.UserCouponMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/feign/coupon")
public class FeignCouponController {

    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private CouponMapper couponMapper;

    @PostMapping("/use")
    public Result<Void> useCoupon(@RequestParam Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        // 仅未使用的券允许核销
        if (userCoupon != null && userCoupon.getStatus() == 0) {
            userCoupon.setStatus(1);
            userCouponMapper.updateById(userCoupon);
        }
        return Result.success();
    }

    @GetMapping("/getUsableCoupon")
    public Result<CouponDTO> getUsableCoupon(@RequestParam Long userCouponId) {
        // 1. 查询用户领取的券
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        // 用户券不存在 或 已使用，直接返回null
        if (userCoupon == null || userCoupon.getStatus() != 0) {
            return Result.success(null);
        }
        // 2. 查询原始优惠券信息
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return Result.success(null);
        }
        // 3. 实体转DTO返回
        CouponDTO dto = new CouponDTO();
        dto.setValue(coupon.getValue());
        dto.setMinAmount(coupon.getMinAmount());
        dto.setStatus(coupon.getStatus());
        dto.setStartTime(coupon.getStartTime());
        dto.setEndTime(coupon.getEndTime());
        return Result.success(dto);
    }
}