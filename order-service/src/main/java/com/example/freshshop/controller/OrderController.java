package com.example.freshshop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Order;
import com.example.freshshop.feign.UserFeignClient;
import com.example.freshshop.service.OrderService;
import com.example.freshshop.utils.JwtUtil;
import com.example.freshshop.vo.OrderConfirmVO;
import com.example.freshshop.vo.OrderVO;
import com.example.freshshop.vo.SalesCategoryVO;
import com.example.freshshop.vo.SalesDailyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final UserFeignClient userFeignClient;

    // 构造注入
    public OrderController(OrderService orderService,
                           UserFeignClient userFeignClient) {
        this.orderService = orderService;
        this.userFeignClient = userFeignClient;
    }

    @Operation(summary = "获取当前用户订单列表（分页）")
    @GetMapping("/list")
    public Result<Page<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.list(page, size, userId);
    }

    @Operation(summary = "订单确认页（根据订单ID加载）")
    @GetMapping("/confirm/info")
    public Result<OrderConfirmVO> confirmInfo(
            @RequestParam Long orderId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.getOrderConfirmInfo(orderId, userId);
    }

    @Operation(summary = "订单支付（支持优惠券）")
    @PostMapping("/pay/submit")
    public Result<Void> payWithCoupon(
            @RequestParam Long orderId,
            @RequestParam(required = false) Long couponId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.payWithCoupon(orderId, userId, couponId);
    }

    @Operation(summary = "订单确认预览（支持优惠券抵扣）")
    @GetMapping("/confirm")
    public Result<OrderConfirmVO> confirm(
            @RequestParam(required = false) Long couponId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.confirm(userId, couponId);
    }

    @Operation(summary = "创建订单（支持优惠券）")
    @PostMapping("/create")
    public Result<Order> create(
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String consignee,
            @RequestParam(required = false) Long couponId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.create(userId, address, phone, consignee, couponId);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel/{id}")
    public Result<?> cancel(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.cancelOrder(id, userId);
    }

    @Operation(summary = "模拟支付订单")
    @PostMapping("/pay/{id}")
    public Result<Void> pay(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.error("订单状态异常，无法支付");
        }
        order.setStatus(1);
        orderService.updateById(order);
        return Result.success();
    }

    // ==========================
    // 管理员接口（权限校验）
    // ==========================
    @Operation(summary = "管理员-获取所有订单列表（分页）")
    @GetMapping("/admin/list")
    public Result<Page<Order>> adminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return orderService.adminList(page, size);
    }

    @Operation(summary = "管理员-每日销售统计")
    @GetMapping("/admin/sales/daily")
    public Result<List<SalesDailyVO>> dailySales(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestHeader("Authorization") String token
    ) {
        return orderService.getDailySales(startDate, endDate);
    }

    @Operation(summary = "管理员-品类销售占比")
    @GetMapping("/admin/sales/category")
    public Result<List<SalesCategoryVO>> categorySales(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestHeader("Authorization") String token
    ) {
        return orderService.getCategorySales(startDate, endDate);
    }
}