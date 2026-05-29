package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.dto.CouponDTO;
import com.example.freshshop.dto.CartDTO;
import com.example.freshshop.dto.GoodsDTO;
import com.example.freshshop.entity.Order;
import com.example.freshshop.entity.OrderItem;
import com.example.freshshop.feign.CategoryFeignClient;
import com.example.freshshop.feign.GoodsFeignClient;
import com.example.freshshop.feign.MarketingFeignClient;
import com.example.freshshop.feign.UserFeignClient;
import com.example.freshshop.mapper.OrderItemMapper;
import com.example.freshshop.mapper.OrderMapper;
import com.example.freshshop.service.OrderService;
import com.example.freshshop.vo.*;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserFeignClient userFeignClient;
    private final GoodsFeignClient goodsFeignClient;
    private final MarketingFeignClient marketingFeignClient;
    private final CategoryFeignClient categoryFeignClient;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            RedissonClient redissonClient,
                            RocketMQTemplate rocketMQTemplate,
                            UserFeignClient userFeignClient,
                            GoodsFeignClient goodsFeignClient,
                            MarketingFeignClient marketingFeignClient,
                            CategoryFeignClient categoryFeignClient) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.redissonClient = redissonClient;
        this.rocketMQTemplate = rocketMQTemplate;
        this.userFeignClient = userFeignClient;
        this.goodsFeignClient = goodsFeignClient;
        this.marketingFeignClient = marketingFeignClient;
        this.categoryFeignClient = categoryFeignClient;
    }

    // RocketMQ 15分钟延迟级别
    private static final int DELAY_LEVEL_15MIN = 16;
    // 订单操作分布式锁前缀
    private static final String ORDER_LOCK_PREFIX = "lock:order:operate:";
    // 新增：下单专用锁前缀（区分订单操作锁，语义更清晰）
    private static final String ORDER_CREATE_LOCK_PREFIX = "lock:order:create:";

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

    /**
     * 支付接口：分布式锁 + 状态校验 + 15分钟超时兜底
     * 防止：支付/超时取消/手动取消 并发冲突、超时订单继续支付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payWithCoupon(Long orderId, Long userId, Long couponId) {
        // 订单全局锁：保证支付、超时取消、手动取消互斥
        String lockKey = ORDER_LOCK_PREFIX + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            return Result.error("订单操作繁忙，请稍后重试");
        }

        try {
            Order order = getOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getId, orderId)
                    .eq(Order::getUserId, userId));

            if (order == null) {
                return Result.error("订单不存在");
            }
            // 状态校验：仅待支付(0)允许支付
            if (order.getStatus() != 0) {
                return Result.error("订单已支付、已取消，无法支付");
            }

            // 硬性15分钟超时兜底（MQ异常/延迟也能拦截）
            LocalDateTime now = LocalDateTime.now();
            long passMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
            if (passMinutes >= 15) {
                return Result.error("订单已超时，无法支付");
            }

            BigDecimal originalAmount = order.getTotalAmount();
            // 未选择优惠券
            if (couponId == null) {
                order.setStatus(1);
                order.setPayTime(now);
                updateById(order);
                return Result.success();
            }

            // 校验优惠券
            CouponDTO couponDTO = marketingFeignClient.getUsableCoupon(couponId).getData();
            if (couponDTO == null) {
                return Result.error("优惠券不可用或已被使用");
            }
            if (!Integer.valueOf(1).equals(couponDTO.getStatus())) {
                return Result.error("优惠券未启用");
            }
            if (now.isBefore(couponDTO.getStartTime()) || now.isAfter(couponDTO.getEndTime())) {
                return Result.error("优惠券不在使用有效期内");
            }
            if (originalAmount.compareTo(couponDTO.getMinAmount()) < 0) {
                return Result.error("订单金额未达到优惠券使用门槛");
            }

            // 计算实付金额
            BigDecimal newAmount = originalAmount.subtract(couponDTO.getValue());
            if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                newAmount = BigDecimal.ZERO;
            }

            // 核销优惠券 + 更新订单
            marketingFeignClient.useCoupon(couponId);
            order.setTotalAmount(newAmount);
            order.setStatus(1);
            order.setPayTime(now);
            updateById(order);

            return Result.success();
        } finally {
            // 释放分布式锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
            List<OrderItem> itemList = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            vo.setItemList(itemList);
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

    /**
     * 创建订单：修正库存MQ消息格式，增加 type 字段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Order> create(Long userId, String address, String phone, String consignee, Long couponId) {
        // ===================== 新增分布式锁开始 =====================
        String lockKey = ORDER_CREATE_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        // 尝试抢锁：不等待，抢到就执行业务，抢不到直接返回
        boolean locked = lock.tryLock();
        if (!locked) {
            return Result.error("请勿重复提交订单，请稍后再试");
        }
        // ===================== 新增分布式锁结束 =====================

        try {
            // 原有全部业务逻辑保持不变
            List<CartDTO> cartList = userFeignClient.getUserCart(userId).getData();
            if (cartList == null || cartList.isEmpty()) {
                return Result.error("购物车为空");
            }

            // 先创建订单，拿到 orderNo 再传参给库存接口
            Order order = new Order();
            order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
            order.setUserId(userId);
            order.setConsignee(consignee);
            order.setPhone(phone);
            order.setAddress(address);
            order.setTotalAmount(BigDecimal.ZERO);
            order.setStatus(0);
            order.setCreateTime(LocalDateTime.now());
            save(order);

            String orderNo = order.getOrderNo();

            // 扣减库存：新增传入 orderNo
            for (CartDTO cart : cartList) {
                Boolean ok = goodsFeignClient.deductStock(cart.getGoodsId(), cart.getNum(), orderNo).getData();
                if (ok == null || !ok) {
                    return Result.error("商品库存不足");
                }
            }

            // 计算商品原始总价（下单阶段不处理优惠券）
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

            // 更新订单总金额
            order.setTotalAmount(totalAmount);
            updateById(order);

            // 保存订单项
            Long oid = order.getId();
            for (OrderItem item : orderItems) {
                item.setOrderId(oid);
                orderItemMapper.insert(item);
            }

            // ========== 已删除：订单服务本地发送库存MQ，统一由商品服务发送 ==========

            // 发送15分钟订单超时延迟消息（保留，和库存无关）
            Message<String> delayMsg = MessageBuilder.withPayload(order.getId().toString()).build();
            rocketMQTemplate.syncSend("order-timeout-topic", delayMsg, 3000, DELAY_LEVEL_15MIN);

            // 清空购物车
            userFeignClient.clearCart(userId);
            return Result.success(order);

        } finally {
            // ===================== 新增释放锁开始 =====================
            // 业务执行完毕，无论成功失败，释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            // ===================== 新增释放锁结束 =====================
        }
    }

    /**
     * 手动取消订单：加分布式锁 + 状态校验，防重复取消、并发冲突
     */
    /**
     * 手动取消订单：加分布式锁 + 状态校验，防重复取消、并发冲突
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelOrder(Long id, Long userId) {
        String lockKey = ORDER_LOCK_PREFIX + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            return Result.error("订单操作繁忙，请稍后重试");
        }

        try {
            Order order = getById(id);
            if (order == null) {
                return Result.error("订单不存在");
            }
            // 仅待支付可取消
            if (order.getStatus() != 0) {
                return Result.error("订单已支付或已取消，无法取消");
            }

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
            );

            String orderNo = order.getOrderNo();
            for (OrderItem item : items) {
                // 回补库存：新增传入 orderNo
                goodsFeignClient.returnStock(item.getGoodsId(), item.getNum(), orderNo);
            }

            // 更新订单为已取消
            order.setStatus(-1);
            updateById(order);
            return Result.success("取消成功，库存已恢复");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Result<Page<Order>> adminList(Integer page, Integer size) {
        return Result.success(page(new Page<>(page, size), null));
    }

    @Override
    public Result<List<SalesDailyVO>> getDailySales(LocalDate startDate, LocalDate endDate) {
        List<Order> orderList = list(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 2)
                .between(Order::getCreateTime, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
        );

        Map<String, SalesDailyVO> dayMap = new LinkedHashMap<>();
        for (Order order : orderList) {
            String day = order.getCreateTime().toLocalDate().toString();
            SalesDailyVO vo = dayMap.getOrDefault(day, new SalesDailyVO());
            vo.setDate(day);
            vo.setTotalSales(vo.getTotalSales() == null ? BigDecimal.ZERO : vo.getTotalSales());
            vo.setTotalSales(vo.getTotalSales().add(order.getTotalAmount()));
            vo.setOrderCount(vo.getOrderCount() == null ? 0 : vo.getOrderCount() + 1);

            List<OrderItem> itemList = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            int totalQty = itemList.stream().mapToInt(OrderItem::getNum).sum();
            vo.setTotalQuantity(vo.getTotalQuantity() == null ? 0 : vo.getTotalQuantity() + totalQty);
            dayMap.put(day, vo);
        }
        return Result.success(new ArrayList<>(dayMap.values()));
    }

    @Override
    public Result<List<SalesCategoryVO>> getCategorySales(LocalDate startDate, LocalDate endDate) {
        List<Order> orderList = list(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 2)
                .between(Order::getCreateTime, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
        );
        if (orderList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> orderIds = orderList.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> itemList = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );

        Map<Long, SalesCategoryVO> categoryMap = new HashMap<>();
        BigDecimal allSales = BigDecimal.ZERO;

        for (OrderItem item : itemList) {
            GoodsDTO goods = goodsFeignClient.getGoods(item.getGoodsId()).getData();
            if (goods == null || goods.getCategoryId() == null) {
                continue;
            }

            Long cid = goods.getCategoryId();
            String categoryName = categoryFeignClient.getCategoryName(cid).getData();
            if (categoryName == null) {
                categoryName = "未知分类";
            }

            SalesCategoryVO vo = categoryMap.getOrDefault(cid, new SalesCategoryVO());
            vo.setCategoryName(categoryName);
            vo.setTotalSales(vo.getTotalSales() == null ? BigDecimal.ZERO : vo.getTotalSales());

            BigDecimal itemSales = item.getPrice().multiply(new BigDecimal(item.getNum()));
            vo.setTotalSales(vo.getTotalSales().add(itemSales));
            vo.setTotalQuantity(vo.getTotalQuantity() == null ? 0 : vo.getTotalQuantity() + item.getNum());

            categoryMap.put(cid, vo);
            allSales = allSales.add(itemSales);
        }

        List<SalesCategoryVO> result = new ArrayList<>(categoryMap.values());
        for (SalesCategoryVO vo : result) {
            if (allSales.compareTo(BigDecimal.ZERO) > 0) {
                vo.setProportion(vo.getTotalSales().divide(allSales, 4, BigDecimal.ROUND_HALF_UP));
            } else {
                vo.setProportion(BigDecimal.ZERO);
            }
        }
        return Result.success(result);
    }
}