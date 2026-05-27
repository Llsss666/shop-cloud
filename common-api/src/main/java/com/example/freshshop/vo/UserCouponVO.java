package com.example.freshshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户优惠券VO")
public class UserCouponVO {

    @Schema(description = "用户优惠券ID")
    private Long id;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "优惠金额")
    private BigDecimal value;

    @Schema(description = "使用门槛")
    private BigDecimal minAmount;

    @Schema(description = "状态 0-未使用 1-已使用 2-已过期")
    private Integer status;

    @Schema(description = "领取时间")
    private LocalDateTime createTime;
}