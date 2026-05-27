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

    // ==========================================
    // 项目启动 → 预加载所有商品库存
    // ==========================================
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

    // ==========================================
    // 商品详情（带分布式锁，防止缓存击穿）
    // ==========================================
    public Result<Goods> getGoodsDetail(Long goodsId) {
        String infoKey = String.format(GOODS_INFO_KEY, goodsId);
        String stockKey = String.format(GOODS_STOCK_KEY, goodsId);
        String lockKey = "lock:goods:detail:" + goodsId;

        // 1. 先查缓存
        String infoJson = redisTemplateDb1.opsForValue().get(infoKey);
        Goods goods;

        if (infoJson == null) {
            // 2. 缓存未命中，加分布式锁
            RLock lock = redissonClient.getLock(lockKey);
            try {
                lock.lock(5, TimeUnit.SECONDS);

                // 3. 双重检查锁
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
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        // 4. 从缓存读取
        try {
            goods = objectMapper.readValue(infoJson, Goods.class);
        } catch (Exception e) {
            goods = getById(goodsId);
        }

        // 5. 覆盖最新库存
        String realStock = redisTemplateDb1.opsForValue().get(stockKey);
        if (realStock != null) {
            goods.setStock(Integer.parseInt(realStock));
        }

        return Result.success(goods);
    }

    // ==========================================
    // 扣库存
    // ==========================================
    // ==========================================
// 扣库存
// ==========================================
    public boolean deductStock(Long goodsId, int num) {
        String stockKey = String.format(GOODS_STOCK_KEY, goodsId);
        Long remain = redisTemplateDb1.opsForValue().decrement(stockKey, num);

        if (remain == null || remain < 0) {
            redisTemplateDb1.opsForValue().increment(stockKey, num);
            return false;
        }

        // 🔥 这里删除旧格式 MQ！完全删掉！
        // rocketMQTemplate.syncSend("stock-update-topic", goodsId + ":" + num);

        return true;
    }

    // ==========================================
    // 取消订单 → 库存回补
    // ==========================================
    public void cancelOrderStockBack(Long goodsId, int num) {
        String stockKey = String.format(GOODS_STOCK_KEY, goodsId);
        redisTemplateDb1.opsForValue().increment(stockKey, num);
        // 🔥 把这行【旧格式MQ】直接删掉！！！
        // rocketMQTemplate.syncSend("stock-update-topic", goodsId + ":+" + num);
    }

    // ==========================================
    // 后台修改商品
    // ==========================================
    @Override
    public boolean updateById(Goods goods) {
        boolean success = super.updateById(goods);
        // 清除旧详情缓存
        String infoKey = String.format(GOODS_INFO_KEY, goods.getId());
        redisTemplateDb1.delete(infoKey);
        // 主动触发单次缓存刷新，也可等待定时任务
        goodsSyncTask.syncGoodsToRedis();

        if (goods.getStock() != null) {
            String stockKey = String.format(GOODS_STOCK_KEY, goods.getId());
            redisTemplateDb1.opsForValue().set(stockKey, String.valueOf(goods.getStock()));
        }
        return success;
    }

    // ==========================================
    // 列表页（无缓存）
    // ==========================================
    @Override
    public Result<Page<Goods>> pageList(Integer page, Integer size, Long categoryId, String name, List<Long> categoryIds) {
        List<Goods> allGoods = new ArrayList<>();
        // 读取Redis全部商品数据
        Map<Object, Object> goodsMap = redisTemplateDb1.opsForHash().entries(GoodsSyncTask.GOODS_LIST_CACHE);
        goodsMap.values().forEach(json -> {
            try {
                Goods goods = objectMapper.readValue(json.toString(), Goods.class);
                allGoods.add(goods);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 筛选过滤
        List<Goods> filterList = allGoods.stream()
                .filter(goods -> goods.getStatus() == 1)
                .filter(goods -> {
                    if(categoryIds == null || categoryIds.isEmpty()) return true;
                    return categoryIds.contains(goods.getCategoryId());
                })
                .filter(goods -> {
                    if(name == null || name.isEmpty()) return true;
                    return goods.getName().contains(name);
                })
                .sorted(Comparator.comparing(Goods::getCreateTime).reversed())
                .collect(Collectors.toList());

        // 分页计算
        int total = filterList.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Goods> pageData = filterList.subList(start, end);

        // 覆盖实时库存
        pageData.forEach(goods -> {
            String stockKey = String.format(GOODS_STOCK_KEY, goods.getId());
            String stock = redisTemplateDb1.opsForValue().get(stockKey);
            if (stock != null) {
                goods.setStock(Integer.parseInt(stock));
            }
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