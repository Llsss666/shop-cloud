package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Cart;
import com.example.freshshop.mapper.CartMapper;
import com.example.freshshop.service.CartService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "购物车管理")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartMapper cartMapper;

    // 获取购物车列表
    @Operation(summary = "获取当前用户购物车列表")
    @GetMapping("/list")
    public Result<?> list(
            @Parameter(description = "登录令牌")
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return cartService.getCartList(userId);
    }

    // 加入购物车
    @Operation(summary = "加入购物车")
    @PostMapping("/add")
    public Result<?> add(
            @Parameter(description = "商品ID")
            @RequestParam Long goodsId,

            @Parameter(description = "数量")
            @RequestParam(defaultValue = "1") Integer num,

            @Parameter(description = "登录令牌")
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return cartService.addCart(userId, goodsId, num);
    }

    // 修改购物车数量 - 修复版
    @Operation(summary = "修改购物车商品数量")
    @PostMapping("/update")
    public Result<?> update(
            @RequestBody CartDTO cartDTO,
            @RequestHeader("Authorization") String token
    ) {
        // 1. 打印前端传过来的原始字符串ID
        System.out.println("前端原始字符串ID：" + cartDTO.getId());

        if (cartDTO.getId() == null || cartDTO.getId().trim().isEmpty()) {
            return Result.error("购物车ID不能为空");
        }

        Long cartId;
        try {
            cartId = Long.parseLong(cartDTO.getId());
            // 2. 打印转换后的Long ID
            System.out.println("转为Long后的ID：" + cartId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return Result.error("ID格式非法");
        }

        String tokenStr = token.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(tokenStr);
        System.out.println("当前登录用户ID：" + userId);

        // 3. 根据ID查询，并打印查询结果
        Cart cart = cartMapper.selectById(cartId);
        System.out.println("根据ID查询到的购物车实体：" + cart);

        if (cart == null) {
            return Result.error("购物车记录不存在");
        }

        if (!cart.getUserId().equals(userId)) {
            return Result.error("无权操作");
        }

        cart.setNum(cartDTO.getQuantity());
        cartMapper.updateById(cart);
        return Result.success("修改成功");
    }
    // 删除购物车
    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{id}")
    public Result<?> delete(
            @Parameter(description = "购物车ID")
            @PathVariable String id,

            @Parameter(description = "登录令牌")
            @RequestHeader("Authorization") String token
    ) {
        // 字符串转Long，处理大数 + 非法格式
        Long cartId;
        try {
            cartId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return Result.error("ID格式错误");
        }

        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return cartService.deleteCart(cartId);
    }

    // 接收参数
    // CartController 内部静态类，专门接收前端传参
    public static class CartDTO {
        // 由 Long 改为 String，接收前端字符串ID，解决大数失真
        private String id;
        private Integer quantity;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}