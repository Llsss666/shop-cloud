package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Order;
import com.example.freshshop.vo.OrderConfirmVO;
import com.example.freshshop.vo.OrderVO;
import com.example.freshshop.vo.SalesCategoryVO;
import com.example.freshshop.vo.SalesDailyVO;

import java.time.LocalDate;
import java.util.List;

// 🔥 这里必须加：extends IService<Order>
public interface OrderService extends IService<Order> {
    Result<Page<OrderVO>> list(Integer page, Integer size, Long userId);
    Result<OrderConfirmVO> confirm(Long userId, Long couponId);
    Result<Order> create(Long userId, String address, String phone, String consignee, Long couponId);
    Result<?> cancelOrder(Long id, Long userId);
    Result<Page<Order>> adminList(Integer page, Integer size);
    Result<List<SalesDailyVO>> getDailySales(LocalDate startDate, LocalDate endDate);
    Result<List<SalesCategoryVO>> getCategorySales(LocalDate startDate, LocalDate endDate);
    Result<OrderConfirmVO> getOrderConfirmInfo(Long orderId, Long userId);
    Result<Void> payWithCoupon(Long orderId, Long userId, Long couponId);
}