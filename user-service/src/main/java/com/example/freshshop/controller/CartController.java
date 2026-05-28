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

    @Autowired // 👈 必须加这个
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

    // ===================== ✅ 修复：修改购物车数量（直接用 mapper，不报错） =====================
    @Operation(summary = "修改购物车商品数量")
    @PostMapping("/update")
    public Result<?> update(
            @RequestBody CartDTO cartDTO,
            @RequestHeader("Authorization") String token
    ) {
        Cart cart = new Cart();
        cart.setId(cartDTO.getId());
        cart.setNum(cartDTO.getQuantity());
        cartMapper.updateById(cart);
        return Result.success("修改成功");
    }

    // 删除购物车
    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{id}")
    public Result<?> delete(
            @Parameter(description = "购物车ID")
            @PathVariable Long id,

            @Parameter(description = "登录令牌")
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return cartService.deleteCart(id);
    }

    // 接收参数
    public static class CartDTO {
        private Long id;
        private Integer quantity;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}