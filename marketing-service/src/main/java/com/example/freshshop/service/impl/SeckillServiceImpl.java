package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Coupon;
import com.example.freshshop.entity.Seckill;
import com.example.freshshop.entity.UserCoupon;
import com.example.freshshop.entity.UserSeckill;
import com.example.freshshop.mapper.CouponMapper;
import com.example.freshshop.mapper.SeckillMapper;
import com.example.freshshop.mapper.UserCouponMapper;
import com.example.freshshop.mapper.UserSeckillMapper;
import com.example.freshshop.mq.SeckillMessage;
import com.example.freshshop.service.SeckillService;
import com.example.freshshop.utils.JwtUtil;
import com.example.freshshop.utils.RedisUtil;
import com.example.freshshop.vo.SeckillVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillServiceImpl extends ServiceImpl<SeckillMapper, Seckill> implements SeckillService {

    private final RedisUtil redisUtil;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final CouponMapper couponMapper;
    private final UserSeckillMapper userSeckillMapper;
    private final UserCouponMapper userCouponMapper;

    private static final String SECKILL_STOCK_KEY = "seckill:stock:";
    private static final String SECKILL_USER_KEY = "seckill:user:";
    private static final String SECKILL_LOCK_KEY = "lock:seckill:";
    private static final String MQ_TOPIC = "seckill-topic";

    public SeckillServiceImpl(RedisUtil redisUtil,
                              RedissonClient redissonClient,
                              RocketMQTemplate rocketMQTemplate,
                              CouponMapper couponMapper,
                              UserSeckillMapper userSeckillMapper,
                              UserCouponMapper userCouponMapper) {
        this.redisUtil = redisUtil;
        this.redissonClient = redissonClient;
        this.rocketMQTemplate = rocketMQTemplate;
        this.couponMapper = couponMapper;
        this.userSeckillMapper = userSeckillMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public Result<List<SeckillVO>> listNow() {
        Long userId = null;
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                String token = attr.getRequest().getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
                }
            }
        } catch (Exception e) {
            userId = null;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Seckill> seckillList = lambdaQuery().eq(Seckill::getStatus, 1).list();
        List<SeckillVO> voList = new ArrayList<>();

        for (Seckill seckill : seckillList) {
            SeckillVO vo = new SeckillVO();
            vo.setId(seckill.getId());
            vo.setCouponId(seckill.getCouponId());
            vo.setStock(seckill.getStock());
            vo.setStatus(seckill.getStatus());
            vo.setStartTime(seckill.getStartTime());
            vo.setEndTime(seckill.getEndTime());

            if (now.isBefore(seckill.getStartTime())) {
                vo.setSeckillStatus("未开始");
            } else if (now.isAfter(seckill.getEndTime())) {
                vo.setSeckillStatus("已结束");
            } else {
                vo.setSeckillStatus("进行中");
            }

            Coupon coupon = couponMapper.selectById(seckill.getCouponId());
            if (coupon != null) {
                vo.setCouponName(coupon.getName());
                vo.setValue(coupon.getValue());
                vo.setMinAmount(coupon.getMinAmount());
            }

            boolean bought = false;
            if (userId != null) {
                bought = userSeckillMapper.selectCount(
                        new LambdaQueryWrapper<UserSeckill>()
                                .eq(UserSeckill::getUserId, userId)
                                .eq(UserSeckill::getSeckillId, seckill.getId())
                ) > 0;
            }
            vo.setBought(bought);
            voList.add(vo);

            redisUtil.set(2, SECKILL_STOCK_KEY + seckill.getId(), String.valueOf(seckill.getStock()), 1, TimeUnit.HOURS);
        }

        return Result.success(voList);
    }

    @Override
    public Result<Void> doSeckill(Long seckillId, Long userId) {
        Seckill seckill = getById(seckillId);
        if (seckill == null) return Result.error("秒杀不存在");

        LocalDateTime now = LocalDateTime.now();
        if (seckill.getStatus() != 1 || now.isBefore(seckill.getStartTime()) || now.isAfter(seckill.getEndTime())) {
            return Result.error("秒杀未开始/已结束");
        }

        RLock lock = redissonClient.getLock(SECKILL_LOCK_KEY + seckillId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                return Result.error("抢购人数过多，请稍后重试");
            }

            String stockKey = SECKILL_STOCK_KEY + seckillId;
            String userKey = SECKILL_USER_KEY + seckillId + ":" + userId;

            Long remain = redisUtil.decrement(2, stockKey);
            if (remain == null || remain < 0) {
                redisUtil.increment(2, stockKey);
                return Result.error("已抢完");
            }

            if (redisUtil.hasKey(2, userKey)) {
                redisUtil.increment(2, stockKey);
                return Result.error("每人限一次");
            }

            redisUtil.set(2, userKey, "1", 24, TimeUnit.HOURS);

            SeckillMessage msg = new SeckillMessage();
            msg.setSeckillId(seckillId);
            msg.setUserId(userId);
            rocketMQTemplate.syncSend(MQ_TOPIC, MessageBuilder.withPayload(msg).build());

            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("系统繁忙");
        } catch (Exception e) {
            redisUtil.increment(2, SECKILL_STOCK_KEY + seckillId);
            redisUtil.delete(2, SECKILL_USER_KEY + seckillId + ":" + userId);
            e.printStackTrace();
            return Result.error("秒杀失败，请重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDbSeckill(SeckillMessage msg) {
        Long seckillId = msg.getSeckillId();
        Long userId = msg.getUserId();

        Long count = userSeckillMapper.selectCount(
                new LambdaQueryWrapper<UserSeckill>()
                        .eq(UserSeckill::getUserId, userId)
                        .eq(UserSeckill::getSeckillId, seckillId)
        );
        if (count > 0) {
            log.info("用户已参与过秒杀，直接忽略：{}", msg);
            return;
        }

        Seckill seckill = getById(seckillId);
        if (seckill == null) return;

        seckill.setStock(seckill.getStock() - 1);
        updateById(seckill);

        UserSeckill userSeckill = new UserSeckill();
        userSeckill.setUserId(userId);
        userSeckill.setSeckillId(seckillId);
        userSeckill.setCreateTime(LocalDateTime.now());
        userSeckillMapper.insert(userSeckill);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(seckill.getCouponId());
        userCoupon.setStatus(0);
        userCoupon.setCreateTime(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
    }

    @Override
    public boolean saveBatch(Collection<Seckill> entityList, int batchSize) {
        return super.saveBatch(entityList, batchSize);
    }

}