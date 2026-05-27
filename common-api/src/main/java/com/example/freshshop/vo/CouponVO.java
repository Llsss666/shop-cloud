package com.example.freshshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "优惠券VO")
public class CouponVO {

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "优惠金额")
    private BigDecimal value;

    @Schema(description = "使用门槛")
    private BigDecimal minAmount;

    @Schema(description = "优惠券类型描述")
    private String typeDesc;
}