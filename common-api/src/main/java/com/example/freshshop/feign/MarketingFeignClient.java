package com.example.freshshop.feign;

import com.example.freshshop.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "marketing-service")
public interface MarketingFeignClient {

    // 优惠券现在属于 marketing-service
    @PostMapping("/feign/coupon/use")
    Result<Void> useCoupon(@RequestParam("couponId") Long couponId);

}