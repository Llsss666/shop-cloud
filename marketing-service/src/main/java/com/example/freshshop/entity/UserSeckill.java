package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSeckill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long seckillId;
    private LocalDateTime createTime;
}