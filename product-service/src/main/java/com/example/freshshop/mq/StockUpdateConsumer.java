package com.example.freshshop.mq;

import com.example.freshshop.mapper.GoodsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
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
    private GoodsMapper goodsMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String MQ_IDEMPOTENT_KEY = "mq:consumed:";

    @Override
    public void onMessage(String message) {
        String[] arr = message.split(":");
        if (arr.length != 3) {
            log.warn("[MQ] 丢弃无效消息：{}", message);
            return;
        }

        String orderNo;
        long goodsId;
        int num;
        try {
            orderNo = arr[0];
            goodsId = Long.parseLong(arr[1]);
            num = Integer.parseInt(arr[2]);
        } catch (Exception e) {
            log.error("[MQ] 解析失败，丢弃：{}", message);
            return;
        }

        // 🔥 修复：幂等key加入 num，区分扣减/回补
        String key = MQ_IDEMPOTENT_KEY + orderNo + ":" + goodsId + ":" + num;
        Boolean consumed = redisTemplate.opsForValue().setIfAbsent(key, "1", 6, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(consumed)) {
            log.warn("[MQ] 重复消费已拦截：{}", message);
            return;
        }

        try {
            if (num < 0) {
                int recover = -num;
                log.info("[MQ] 回补库存 → 商品{}:{}", goodsId, recover);
                goodsMapper.increaseStock(goodsId, recover);
            } else {
                log.info("[MQ] 扣减库存 → 商品{}:{}", goodsId, num);
                goodsMapper.deductStock(goodsId, num);
            }
        } catch (Exception e) {
            redisTemplate.delete(key);
            log.error("[MQ] 库存更新失败", e);
        }
    }
}