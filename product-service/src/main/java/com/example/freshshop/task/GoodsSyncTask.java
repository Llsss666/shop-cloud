package com.example.freshshop.task;

import com.example.freshshop.entity.Goods;
import com.example.freshshop.mapper.GoodsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoodsSyncTask {
    // 列表缓存key前缀
    public static final String GOODS_LIST_CACHE = "goods:all:list";
    // 单商品详情缓存
    public static final String GOODS_INFO_KEY = "goods:info:%d";

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    @Qualifier("redisTemplateDb1")
    private StringRedisTemplate redisTemplateDb1;

    @Autowired
    private ObjectMapper objectMapper;

    // 每5分钟同步一次商品全量数据
    @Scheduled(fixedRate = 300000)
    public void syncGoodsToRedis() {
        List<Goods> goodsList = goodsMapper.selectList(null);
        // 清空旧列表缓存
        redisTemplateDb1.delete(GOODS_LIST_CACHE);
        // 批量写入Redis
        goodsList.forEach(goods -> {
            try {
                String json = objectMapper.writeValueAsString(goods);
                redisTemplateDb1.opsForHash().put(GOODS_LIST_CACHE, goods.getId().toString(), json);
                // 同步单条详情缓存
                redisTemplateDb1.opsForValue().set(String.format(GOODS_INFO_KEY,goods.getId()),json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println("商品数据定时同步Redis完成");
    }
}