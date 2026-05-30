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
import io.seata.spring.annotation.GlobalTransactional;
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
import java.util.concurrent.TimeUnit;
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
    // 下单专用锁前缀
    private static final String ORDER_CREATE_LOCK_PREFIX = "lock:order:create:";
    // 锁持有超时时间(根据业务调优，这里改为5s)
    private static final long LOCK_WAIT_TIME = 0;
    private static final long LOCK_HOLD_TIME = 5;

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
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payWithCoupon(Long orderId, Long userId, Long couponId) {
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
            if (order.getStatus() != 0) {
                return Result.error("订单已支付、已取消，无法支付");
            }

            LocalDateTime now = LocalDateTime.now();
            long passMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
            if (passMinutes >= 15) {
                return Result.error("订单已超时，无法支付");
            }

            BigDecimal originalAmount = order.getTotalAmount();
            if (couponId == null) {
                order.setStatus(1);
                order.setPayTime(now);
                updateById(order);
                return Result.success();
            }

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

            BigDecimal newAmount = originalAmount.subtract(couponDTO.getValue());
            if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                newAmount = BigDecimal.ZERO;
            }

            marketingFeignClient.useCoupon(couponId);
            order.setTotalAmount(newAmount);
            order.setStatus(1);
            order.setPayTime(now);
            updateById(order);

            return Result.success();
        } finally {
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
     * 外层入口方法：事务执行成功后，再发送MQ（MQ完全隔离在事务外部）
     */
    /**
     * 下单入口：事务成功后发【订单超时MQ + 库存扣减MQ】
     */
    @Override
    public Result<Order> create(Long userId, String address, String phone, String consignee, Long couponId) {
        // 关键：事务执行前先拿到购物车快照
        List<CartDTO> cartList = userFeignClient.getUserCart(userId).getData();
        System.out.println("下单前购物车数据：" + cartList);

        Result<Order> transactionResult = createOrderTransactional(userId, address, phone, consignee, couponId);
        if (transactionResult.getCode() != 200 || transactionResult.getData() == null) {
            return transactionResult;
        }

        Order order = transactionResult.getData();
        String orderNo = order.getOrderNo();
        Long orderId = order.getId();

        // 1. 订单延迟消息
        Message<String> delayMsg = MessageBuilder.withPayload(orderId.toString()).build();
        rocketMQTemplate.syncSend("order-timeout-topic", delayMsg, 3000, DELAY_LEVEL_15MIN);

        // 2. 使用提前拿到的购物车数据发MQ，不再重复查询
        if (cartList != null && !cartList.isEmpty()) {
            for (CartDTO cart : cartList) {
                try {
                    goodsFeignClient.sendDeductMq(cart.getGoodsId(), cart.getNum(), orderNo);
                    System.out.println("已调用扣库存MQ，商品ID：" + cart.getGoodsId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("购物车无数据，跳过库存MQ");
        }

        return transactionResult;
    }

    /**
     * 纯分布式事务方法：下单核心逻辑（无任何MQ）
     */
    @GlobalTransactional(rollbackFor = Throwable.class)
    public Result<Order> createOrderTransactional(Long userId, String address, String phone, String consignee, Long couponId) {
        String lockKey = ORDER_CREATE_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_HOLD_TIME, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error("请勿重复提交订单，请稍后再试");
            }

            List<CartDTO> cartList = userFeignClient.getUserCart(userId).getData();
            if (cartList == null || cartList.isEmpty()) {
                throw new RuntimeException("购物车为空");
            }

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

            // 扣减库存（纯Redis操作，无MQ）
            for (CartDTO cart : cartList) {
                Boolean ok = goodsFeignClient.deductStock(cart.getGoodsId(), cart.getNum(), orderNo).getData();
                if (ok == null || !ok) {
                    throw new RuntimeException("商品库存不足");
                }
            }

            // 计算金额、封装订单项
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

            order.setTotalAmount(totalAmount);
            updateById(order);

            Long oid = order.getId();
            for (OrderItem item : orderItems) {
                item.setOrderId(oid);
                orderItemMapper.insert(item);
            }

            // 清空购物车
            userFeignClient.clearCart(userId);

            return Result.success(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("下单请求中断，请稍后重试");
        } catch (Exception e) {
            throw new RuntimeException("下单失败", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

// ======================================================

    /**
     * 取消订单入口：事务成功后发送库存回补MQ
     */
    @Override
    public Result<?> cancelOrder(Long id, Long userId) {
        Result<?> transResult = cancelOrderTransactional(id, userId);
        if (transResult.getCode() != 200) {
            return transResult;
        }

        // 事务提交成功，事务外发送库存回补MQ
        Order order = getById(id);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
        );
        String orderNo = order.getOrderNo();
        for (OrderItem item : items) {
            goodsFeignClient.sendAddMq(item.getGoodsId(), item.getNum(), orderNo);
        }

        return transResult;
    }

    /**
     * 纯分布式事务方法：取消订单核心逻辑（无任何MQ）
     */
    @GlobalTransactional(rollbackFor = Throwable.class)
    public Result<?> cancelOrderTransactional(Long id, Long userId) {
        String lockKey = ORDER_LOCK_PREFIX + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_HOLD_TIME, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error("订单操作繁忙，请稍后重试");
            }

            Order order = getById(id);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            if (order.getStatus() != 0) {
                throw new RuntimeException("订单已支付或已取消，无法取消");
            }

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
            );
            String orderNo = order.getOrderNo();

            // 回补库存（纯Redis操作，无MQ）
            for (OrderItem item : items) {
                goodsFeignClient.returnStock(item.getGoodsId(), item.getNum(), orderNo);
            }

            order.setStatus(-1);
            updateById(order);

            return Result.success("取消成功，库存已恢复");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("订单操作中断，请稍后重试");
        } catch (Exception e) {
            throw new RuntimeException("取消订单失败", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
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