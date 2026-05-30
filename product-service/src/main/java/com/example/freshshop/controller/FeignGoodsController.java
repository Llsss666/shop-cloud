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

    public FeignGoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping("/get")
    public Result<Goods> getGoods(@RequestParam("goodsId") Long goodsId) {
        return goodsService.getGoodsDetail(goodsId);
    }

    @PostMapping("/deductStock")
    public Result<Boolean> deductStock(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo) {
        boolean res = goodsService.deductStock(goodsId, num, orderNo);
        return Result.success(res);
    }

    @PostMapping("/returnStock")
    public Result<Void> returnStock(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo) {
        goodsService.cancelOrderStockBack(goodsId, num, orderNo);
        return Result.success();
    }

    @PostMapping("/sendDeductMq")
    public Result<Void> sendDeductMq(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo) {
        goodsService.sendDeductStockMq(goodsId, num, orderNo);
        return Result.success();
    }

    @PostMapping("/sendAddMq")
    public Result<Void> sendAddMq(
            @RequestParam("goodsId") Long goodsId,
            @RequestParam("num") Integer num,
            @RequestParam("orderNo") String orderNo) {
        goodsService.sendAddStockMq(goodsId, num, orderNo);
        return Result.success();
    }
}