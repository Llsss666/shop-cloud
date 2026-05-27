package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Coupon;
import com.example.freshshop.vo.UserCouponVO;

import java.util.List;

public interface CouponService extends IService<Coupon> {
    Result<List<Coupon>> listAll();
    Result<Void> receive(Long couponId, Long userId);

    // 用户自己的未使用优惠券
    Result<List<UserCouponVO>> myCoupons(Long userId);
}