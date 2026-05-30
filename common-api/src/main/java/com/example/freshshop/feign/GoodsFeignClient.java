package com.example.freshshop.feign;

import com.example.freshshop.common.Result;
import com.example.freshshop.dto.GoodsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", contextId = "goodsFeign")
public interface GoodsFeignClient {

    @GetMapping("/feign/goods/get")
    Result<GoodsDTO> getGoods(@RequestParam("goodsId") Long goodsId);

    // 事务内：纯扣库存
    @PostMapping("/feign/goods/deductStock")
    Result<Boolean> deductStock(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo);

    // 事务内：纯回补库存
    @PostMapping("/feign/goods/returnStock")
    Result<Void> returnStock(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo);

    // 事务外：发送扣库存MQ
    @PostMapping("/feign/goods/sendDeductMq")
    Result<Void> sendDeductMq(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo);

    // 事务外：发送回补库存MQ
    @PostMapping("/feign/goods/sendAddMq")
    Result<Void> sendAddMq(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo);
}