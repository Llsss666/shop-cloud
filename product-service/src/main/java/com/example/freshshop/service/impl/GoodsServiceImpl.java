package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Goods;
import com.example.freshshop.mapper.GoodsMapper;
import com.example.freshshop.service.GoodsService;
import com.example.freshshop.task.GoodsSyncTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Autowired
    @Qualifier("redisTemplateDb1")
    private StringRedisTemplate redisTemplateDb1;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GoodsSyncTask goodsSyncTask;

    public static final String GOODS_INFO_KEY = "goods:info:%d";
    public static final String GOODS_STOCK_KEY = "goods:stock:%d";
    public static final long GOODS_INFO_EXPIRE = 1;

    @PostConstruct
    public void initAllStockToRedis() {
        try {
            System.out.println("🚀 初始化商品库存到 Redis DB1");
            List<Goods> goodsList = list();
            for (Goods goods : goodsList) {
                String stockKey = String.format(GOODS_STOCK_KEY, goods.getId());
                redisTemplateDb1.opsForValue().set(stockKey, String.valueOf(goods.getStock()));
            }
            System.out.println("✅ 库存初始化完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Result<Goods> getGoodsDetail(Long goodsId) {
        String infoKey = String.format(GOODS_INFO_KEY, goodsId);
        String stockKey = String.format(GOODS_STOCK_KEY, goodsId);
        String lockKey = "lock:goods:detail:" + goodsId;

        String infoJson = redisTemplateDb1.opsForValue().get(infoKey);
        Goods goods;

        if (infoJson == null) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                lock.lock(5, TimeUnit.SECONDS);
                infoJson = redisTemplateDb1.opsForValue().get(infoKey);
                if (infoJson == null) {
                    goods = getById(goodsId);
                    if (goods == null) return Result.error("商品不存在");

                    try {
                        String json = objectMapper.writeValueAsString(goods);
                        redisTemplateDb1.opsForValue().set(infoKey, json, GOODS_INFO_EXPIRE, TimeUnit.HOURS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        }

        try {
            goods = objectMapper.readValue(infoJson, Goods.class);
        } catch (Exception e) {
            goods = getById(goodsId);
        }

        String realStock = redisTemplateDb1.opsForValue().get(stockKey);
        if (realStock != null) {
            goods.setStock(Integer.parseInt(realStock));
        }

        return Result.success(goods);
    }

    // -------------------------- 纯库存操作（无MQ，实现接口） --------------------------
    @Override
    public boolean deductStock(Long goodsId, int num, String orderNo) {
        String stockKey = String.format(GOODS_STOCK_KEY, goodsId);
        Long remain = redisTemplateDb1.opsForValue().decrement(stockKey, num);

        if (remain == null || remain < 0) {
            redisTemplateDb1.opsForValue().increment(stockKey, num);
            return false;
        }
        return true;
    }

    @Override
    public void cancelOrderStockBack(Long goodsId, int num, String orderNo) {
        String stockKey = String.format(GOODS_STOCK_KEY, goodsId);
        redisTemplateDb1.opsForValue().increment(stockKey, num);
    }

    // -------------------------- 单独发送MQ（实现接口） --------------------------
    @Override
    public void sendDeductStockMq(Long goodsId, int num, String orderNo) {
        String msg = orderNo + ":" + goodsId + ":" + num + ":deduct";
        try {
            rocketMQTemplate.syncSend("stock-update-topic", MessageBuilder.withPayload(msg).build());
            System.out.println("✅ MQ 扣库存消息：" + msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendAddStockMq(Long goodsId, int num, String orderNo) {
        String msg = orderNo + ":" + goodsId + ":" + num + ":add";
        try {
            rocketMQTemplate.syncSend("stock-update-topic", MessageBuilder.withPayload(msg).build());
            System.out.println("✅ MQ 回补库存消息：" + msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean updateById(Goods goods) {
        boolean success = super.updateById(goods);
        String infoKey = String.format(GOODS_INFO_KEY, goods.getId());
        redisTemplateDb1.delete(infoKey);
        goodsSyncTask.syncGoodsToRedis();

        if (goods.getStock() != null) {
            String stockKey = String.format(GOODS_STOCK_KEY, goods.getId());
            redisTemplateDb1.opsForValue().set(stockKey, String.valueOf(goods.getStock()));
        }
        return success;
    }

    @Override
    public Result<Page<Goods>> pageList(Integer page, Integer size, Long categoryId, String name, List<Long> categoryIds) {
        List<Goods> allGoods = new ArrayList<>();
        Map<Object, Object> goodsMap = redisTemplateDb1.opsForHash().entries(GoodsSyncTask.GOODS_LIST_CACHE);
        goodsMap.values().forEach(json -> {
            try {
                Goods goods = objectMapper.readValue(json.toString(), Goods.class);
                allGoods.add(goods);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        List<Goods> filterList = allGoods.stream()
                .filter(goods -> goods.getStatus() == 1)
                .filter(goods -> categoryIds == null || categoryIds.isEmpty() || categoryIds.contains(goods.getCategoryId()))
                .filter(goods -> name == null || name.isEmpty() || goods.getName().contains(name))
                .sorted(Comparator.comparing(Goods::getCreateTime).reversed())
                .collect(Collectors.toList());

        int total = filterList.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Goods> pageData = filterList.subList(start, end);

        pageData.forEach(goods -> {
            String stockKey = String.format(GOODS_STOCK_KEY, goods.getId());
            String stock = redisTemplateDb1.opsForValue().get(stockKey);
            if (stock != null) goods.setStock(Integer.parseInt(stock));
        });

        Page<Goods> pageResult = new Page<>(page, size, total);
        pageResult.setRecords(pageData);
        return Result.success(pageResult);
    }

    @Override
    public Result<Void> updateStatus(Long id, Integer status) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setStatus(status);
        updateById(goods);
        return Result.success();
    }
}