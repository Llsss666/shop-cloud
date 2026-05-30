package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Goods;

import java.util.List;

public interface GoodsService extends IService<Goods> {
    // 商品详情（带缓存）
    Result<Goods> getGoodsDetail(Long goodsId);
    Result<Page<Goods>> pageList(Integer page, Integer size, Long categoryId, String name, List<Long> categoryIds);
    Result<Void> updateStatus(Long id, Integer status);

    // 原有方法：纯库存操作（无MQ，事务内Feign调用）
    boolean deductStock(Long goodsId, int num, String orderNo);
    void cancelOrderStockBack(Long goodsId, int num, String orderNo);

    // 新增：单独发送MQ（事务外部调用）
    void sendDeductStockMq(Long goodsId, int num, String orderNo);
    void sendAddStockMq(Long goodsId, int num, String orderNo);
}