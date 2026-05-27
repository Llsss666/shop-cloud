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
    // ========== 新增以下两个方法 ==========
    boolean deductStock(Long goodsId, int num);
    void cancelOrderStockBack(Long goodsId, int num);
}