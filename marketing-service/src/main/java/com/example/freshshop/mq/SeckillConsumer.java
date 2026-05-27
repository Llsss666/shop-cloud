package com.example.freshshop.mq;

import com.example.freshshop.service.impl.SeckillServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
        topic = "seckill-topic",
        consumerGroup = "seckill-consumer-group"
)
public class SeckillConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private SeckillServiceImpl seckillService;

    @Override
    public void onMessage(SeckillMessage message) {
        log.info("收到秒杀消息：{}", message);
        seckillService.updateDbSeckill(message);
    }
}