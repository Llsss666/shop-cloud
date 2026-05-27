package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.dto.GoodsDTO;
import com.example.freshshop.entity.Cart;
import com.example.freshshop.feign.GoodsFeignClient;
import com.example.freshshop.mapper.CartMapper;
import com.example.freshshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    // ================================
    // 🔥 只允许注入购物车自己的 Mapper
    // ================================
    @Autowired
    private CartMapper cartMapper;

    // ================================
    // 🔥 跨服务拿商品 → 用 FEIGN
    // ================================
    @Autowired
    private GoodsFeignClient goodsFeignClient;

    // ===================== 购物车列表（改造后） =====================
    @Override
    public Result<?> getCartList(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> cartList = list(wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Cart cart : cartList) {
            // ================================
            // 🔥 Feign 调用商品服务获取商品信息
            // ================================
            GoodsDTO goods = goodsFeignClient.getGoods(cart.getGoodsId()).getData();
            if (goods == null) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("id", cart.getId());
            map.put("userId", cart.getUserId());
            map.put("goodsId", goods.getId());
            map.put("goodsName", goods.getName());
            map.put("goodsImage", goods.getImage());
            map.put("price", goods.getPrice());
            map.put("num", cart.getNum());
            map.put("total", goods.getPrice().multiply(new BigDecimal(cart.getNum())));

            resultList.add(map);
            totalPrice = totalPrice.add((BigDecimal) map.get("total"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cartList", resultList);
        result.put("totalPrice", totalPrice);
        return Result.success(result);
    }

    // ===================== 加入购物车（改造后） =====================
    @Override
    public Result<?> addCart(Long userId, Long goodsId, Integer num) {
        // ================================
        // 🔥 Feign 获取商品
        // ================================
        GoodsDTO goods = goodsFeignClient.getGoods(goodsId).getData();
        if (goods == null) {
            return Result.error("商品不存在");
        }

        if (num > goods.getStock()) {
            return Result.error("商品库存不足，当前库存：" + goods.getStock());
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId).eq(Cart::getGoodsId, goodsId);
        Cart existCart = getOne(wrapper);

        if (existCart != null) {
            int newNum = existCart.getNum() + num;
            if (newNum > goods.getStock()) {
                return Result.error("商品库存不足");
            }
            existCart.setNum(newNum);
            updateById(existCart);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setGoodsId(goodsId);
            cart.setNum(num);
            save(cart);
        }
        return Result.success("加入购物车成功");
    }

    // ===================== 删除购物车 =====================
    @Override
    public Result<?> deleteCart(Long id) {
        removeById(id);
        return Result.success("删除成功");
    }
}