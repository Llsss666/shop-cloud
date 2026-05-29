package com.example.freshshop.feign;

import com.example.freshshop.common.Result;
import com.example.freshshop.dto.CouponDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "marketing-service")
public interface MarketingFeignClient {

    /**
     * 核销用户优惠券
     */
    @PostMapping("/feign/coupon/use")
    Result<Void> useCoupon(@RequestParam("userCouponId") Long userCouponId);

    /**
     * 查询优惠券可用信息（门槛、金额、时间、状态）
     * @param userCouponId 用户领取的优惠券ID
     */
    @GetMapping("/feign/coupon/getUsableCoupon")
    Result<CouponDTO> getUsableCoupon(@RequestParam("userCouponId") Long userCouponId);
}