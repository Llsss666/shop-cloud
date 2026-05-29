package com.example.freshshop.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RocketMQMessageListener(
        topic = "stock-update-topic",
        consumerGroup = "stock-consumer-group-v2",
        consumeThreadNumber = 1
)
public class StockUpdateConsumer implements RocketMQListener<String> {

    @Autowired
    private com.example.freshshop.mapper.GoodsMapper goodsMapper;

    // 注入 4号库 RedisTemplate，专门用于MQ幂等 & 库存锁
    @Autowired
    @Qualifier("redisTemplateDb4")
    private StringRedisTemplate redisTemplate;

    // 幂等前缀 + 分布式锁前缀
    private static final String MQ_IDEMPOTENT_PREFIX = "mq:consumed:";
    private static final String STOCK_LOCK_PREFIX = "lock:stock:";
    // 幂等有效期6小时
    private static final long IDEMPOTENT_EXPIRE_HOUR = 6;
    // 库存锁超时时间 30秒，防止死锁
    private static final long LOCK_WAIT_SECOND = 30;

    // Lua脚本：原子加锁
    private static final DefaultRedisScript<Long> LOCK_SCRIPT = new DefaultRedisScript<>();
    static {
        LOCK_SCRIPT.setScriptText(
                "if redis.call('exists', KEYS[1]) == 0 then " +
                        "redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2]) " +
                        "return 1 " +
                        "else return 0 end"
        );
        LOCK_SCRIPT.setResultType(Long.class);
    }

    // Lua脚本：原子解锁
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();
    static {
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void onMessage(String message) {
        log.info("[MQ] 收到库存消息：{}", message);
        // 消息格式：orderNo:goodsId:num:type  共4段
        String[] arr = message.split(":");
        if (arr.length != 4) {
            log.error("[MQ] 消息格式非法，丢弃：{}", message);
            return;
        }

        String orderNo;
        long goodsId;
        int num;
        String type;
        try {
            orderNo = arr[0];
            goodsId = Long.parseLong(arr[1]);
            num = Integer.parseInt(arr[2]);
            type = arr[3];
        } catch (Exception e) {
            log.error("[MQ] 字段解析失败，丢弃：{}", message, e);
            return;
        }

        // ========= 核心修改：幂等Key 增加操作类型type，区分 deduct / add =========
        String idempotentKey = MQ_IDEMPOTENT_PREFIX + orderNo + ":" + goodsId + ":" + type;
        Boolean isFirstConsume = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_EXPIRE_HOUR, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isFirstConsume)) {
            log.warn("[MQ] 重复消息拦截，订单:{}, 商品:{}, 操作:{}", orderNo, goodsId, type);
            return;
        }

        // 2. 分布式锁：防同商品并发扣库存
        String lockKey = STOCK_LOCK_PREFIX + goodsId;
        String lockValue = String.valueOf(System.currentTimeMillis());
        boolean lockSuccess = acquireLock(lockKey, lockValue, LOCK_WAIT_SECOND);
        if (!lockSuccess) {
            log.error("[MQ] 获取库存锁失败，商品{}，消息丢弃", goodsId);
            redisTemplate.delete(idempotentKey);
            return;
        }

        try {
            // 3. 执行库存操作
            if ("deduct".equals(type)) {
                log.info("[MQ] 扣减库存 订单{} 商品{} 数量{}", orderNo, goodsId, num);
                goodsMapper.deductStock(goodsId, num);
            } else if ("add".equals(type)) {
                log.info("[MQ] 回补库存 订单{} 商品{} 数量{}", orderNo, goodsId, num);
                goodsMapper.increaseStock(goodsId, num);
            } else {
                log.warn("[MQ] 未知操作类型:{}，消息丢弃", type);
                redisTemplate.delete(idempotentKey);
            }
        } catch (Exception e) {
            // 执行异常：删除幂等标记，允许MQ重试
            redisTemplate.delete(idempotentKey);
            log.error("[MQ] 库存操作异常 订单{} 商品{}", orderNo, goodsId, e);
        } finally {
            // 4. 释放分布式锁
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 原子加锁
     */
    private boolean acquireLock(String key, String value, long expireSecond) {
        Long result = redisTemplate.execute(
                LOCK_SCRIPT,
                Collections.singletonList(key),
                value, String.valueOf(expireSecond)
        );
        return result != null && result == 1;
    }

    /**
     * 原子解锁
     */
    private void releaseLock(String key, String value) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
    }
}