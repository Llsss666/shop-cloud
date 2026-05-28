package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Goods;
import com.example.freshshop.service.GoodsService;
import org.springframework.web.bind.annotation.*;

/**
 * 微服务内部调用专用（Feign）
 * 不对外暴露、不校验Token、不校验权限
 */
@RestController
@RequestMapping("/feign/goods")
public class FeignGoodsController {

    private final GoodsService goodsService;

    // 构造注入
    public FeignGoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    /**
     * 根据ID获取商品信息（Feign调用）
     */
    @GetMapping("/get")
    public Result<Goods> getGoods(@RequestParam("goodsId") Long goodsId) {
        return goodsService.getGoodsDetail(goodsId);
    }

    /**
     * 扣减库存（Feign调用）
     */
    @PostMapping("/deductStock")
    public Result<Boolean> deductStock(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num) {
        boolean success = goodsService.deductStock(goodsId, num);
        return Result.success(success);
    }

    /**
     * 返还/回补库存（Feign调用）
     */
    @PostMapping("/returnStock")
    public Result<Void> returnStock(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num) {
        goodsService.cancelOrderStockBack(goodsId, num);
        return Result.success();
    }
}