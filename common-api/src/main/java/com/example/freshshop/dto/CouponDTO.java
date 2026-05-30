package com.example.freshshop.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券公共DTO，存放于common，所有微服务可引用
 */
@Data
public class CouponDTO {
    /** 优惠金额 */
    private BigDecimal value;
    /** 使用门槛 */
    private BigDecimal minAmount;
    /** 优惠券状态 0-关闭 1-开启 */
    private Integer status;
    /** 生效时间 */
    private LocalDateTime startTime;
    /** 失效时间 */
    private LocalDateTime endTime;
}