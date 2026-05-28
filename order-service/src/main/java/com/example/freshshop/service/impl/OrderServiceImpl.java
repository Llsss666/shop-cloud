package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.dto.CartDTO;
import com.example.freshshop.dto.GoodsDTO;
import com.example.freshshop.entity.Order;
import com.example.freshshop.entity.OrderItem;
import com.example.freshshop.feign.GoodsFeignClient;
import com.example.freshshop.feign.MarketingFeignClient;
import com.example.freshshop.feign.UserFeignClient;
import com.example.freshshop.mapper.OrderMapper;
import com.example.freshshop.mapper.OrderItemMapper;
import com.example.freshshop.service.OrderService;
import com.example.freshshop.vo.*;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserFeignClient userFeignClient;
    private final GoodsFeignClient goodsFeignClient;

    // ====================== 🔥 注入新增的 MarketingFeignClient ======================
    private final MarketingFeignClient marketingFeignClient;

    // ====================== 🔥 构造器已更新 ======================
    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            RedissonClient redissonClient,
                            RocketMQTemplate rocketMQTemplate,
                            UserFeignClient userFeignClient,
                            GoodsFeignClient goodsFeignClient,
                            MarketingFeignClient marketingFeignClient) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.redissonClient = redissonClient;
        this.rocketMQTemplate = rocketMQTemplate;
        this.userFeignClient = userFeignClient;
        this.goodsFeignClient = goodsFeignClient;
        this.marketingFeignClient = marketingFeignClient;
    }

    private static final int DELAY_LEVEL_15MIN = 16;

    @Override
    public Result<OrderConfirmVO> getOrderConfirmInfo(Long orderId, Long userId) {
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId));

        if (order == null) {
            return Result.error("订单不存在");
        }

        OrderConfirmVO vo = new OrderConfirmVO();
        vo.setOrderId(order.getId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setAddress(order.getAddress());
        vo.setConsignee(order.getConsignee());
        vo.setPhone(order.getPhone());

        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payWithCoupon(Long orderId, Long userId, Long couponId) {
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId));

        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 0) return Result.error("订单已支付或已取消");

        // ====================== 🔥 优惠券从 marketing-service 调用 ======================
        if (couponId != null) {
            marketingFeignClient.useCoupon(couponId);
        }

        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        updateById(order);

        return Result.success();
    }

    @Override
    public Result<Page<OrderVO>> list(Integer page, Integer size, Long userId) {
        Page<Order> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);

        Page<Order> result = page(pageObj, wrapper);
        List<OrderVO> voList = new ArrayList<>();

        for (Order order : result.getRecords()) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            voList.add(vo);
        }

        Page<OrderVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @Override
    public Result<OrderConfirmVO> confirm(Long userId, Long couponId) {
        return Result.success(new OrderConfirmVO());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Order> create(Long userId, String address, String phone, String consignee, Long couponId) {
        // 1. 获取购物车
        List<CartDTO> cartList = userFeignClient.getUserCart(userId).getData();
        if (cartList == null || cartList.isEmpty()) {
            return Result.error("购物车为空");
        }

        // 2. 预扣库存
        for (CartDTO cart : cartList) {
            Boolean ok = goodsFeignClient.deductStock(cart.getGoodsId(), cart.getNum()).getData();
            if (ok == null || !ok) {
                return Result.error("商品库存不足");
            }
        }

        // 3. 计算价格
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartDTO cart : cartList) {
            GoodsDTO goods = goodsFeignClient.getGoods(cart.getGoodsId()).getData();

            BigDecimal itemTotal = goods.getPrice().multiply(new BigDecimal(cart.getNum()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = new OrderItem();
            item.setGoodsId(goods.getId());
            item.setGoodsName(goods.getName());
            item.setGoodsImage(goods.getImage());
            item.setPrice(goods.getPrice());
            item.setNum(cart.getNum());
            orderItems.add(item);
        }

        // 4. 创建订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setConsignee(consignee);
        order.setPhone(phone);
        order.setAddress(address);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        save(order);

        // 5. 订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 6. 延迟消息
        Message<String> msg = MessageBuilder.withPayload(order.getId().toString()).build();
        rocketMQTemplate.syncSend("order-timeout-topic", msg, 3000, DELAY_LEVEL_15MIN);

        // 7. 清空购物车
        userFeignClient.clearCart(userId);

        return Result.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelOrder(Long id, Long userId) {
        Order order = getById(id);
        if (order == null) return Result.error("订单不存在");
        if (order.getStatus() != 0) return Result.error("无法取消");

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
        );

        for (OrderItem item : items) {
            goodsFeignClient.returnStock(item.getGoodsId(), item.getNum());
        }

        order.setStatus(-1);
        updateById(order);
        return Result.success("取消成功");
    }

    @Override
    public Result<Page<Order>> adminList(Integer page, Integer size) {
        return Result.success(page(new Page<>(page, size), null));
    }

    @Override
    public Result<List<SalesDailyVO>> getDailySales(LocalDate startDate, LocalDate endDate) {
        return Result.success(new ArrayList<>());
    }

    @Override
    public Result<List<SalesCategoryVO>> getCategorySales(LocalDate startDate, LocalDate endDate) {
        return Result.success(new ArrayList<>());
    }
}