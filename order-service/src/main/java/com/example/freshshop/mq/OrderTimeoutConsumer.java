package com.example.freshshop.mq;

import com.example.freshshop.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "order-timeout-topic",
        consumerGroup = "order-timeout-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<String> {

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(String orderId) {
        // 超时自动取消，userId传null即可
        orderService.cancelOrder(Long.parseLong(orderId), null);
    }
}