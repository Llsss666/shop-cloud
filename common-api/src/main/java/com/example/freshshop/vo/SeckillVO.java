package com.example.freshshop.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillVO {
    private Long id;
    private Long couponId;
    private Integer stock;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String seckillStatus;
    // 优惠券信息
    private String couponName;
    private BigDecimal value;
    private BigDecimal minAmount;
    private Boolean bought;
}