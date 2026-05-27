package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Seckill;
import com.example.freshshop.vo.SeckillVO;
import java.util.List;

public interface SeckillService extends IService<Seckill> {
    Result<List<SeckillVO>> listNow(); // 返回 VO
    Result<Void> doSeckill(Long seckillId, Long userId);
}