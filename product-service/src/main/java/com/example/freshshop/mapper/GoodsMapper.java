package com.example.freshshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.freshshop.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    // 异步扣库存
    @Update("UPDATE goods SET stock = stock - #{num} WHERE id = #{goodsId}")
    int deductStock(@Param("goodsId") Long goodsId, @Param("num") int num);

    // 异步加回库存（取消订单用）
    @Update("UPDATE goods SET stock = stock + #{num} WHERE id = #{goodsId}")
    int increaseStock(@Param("goodsId") Long goodsId, @Param("num") int num);
}