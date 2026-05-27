package com.example.freshshop.feign;

import com.example.freshshop.common.Result;
import com.example.freshshop.dto.GoodsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface GoodsFeignClient {

    @GetMapping("/feign/goods/get")
    Result<GoodsDTO> getGoods(@RequestParam("goodsId") Long goodsId);

    @PostMapping("/feign/goods/deductStock")
    Result<Boolean> deductStock(@RequestParam("goodsId") Long goodsId,
                                @RequestParam("num") Integer num);

    @PostMapping("/feign/goods/returnStock")
    Result<Void> returnStock(@RequestParam("goodsId") Long goodsId,
                             @RequestParam("num") Integer num);
}