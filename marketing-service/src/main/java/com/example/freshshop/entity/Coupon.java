package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
@Schema(description = "优惠券实体")
public class Coupon {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "优惠券类型 1-满减券")
    private Integer type;

    @Schema(description = "优惠金额")
    private BigDecimal value;

    @Schema(description = "使用门槛金额")
    private BigDecimal minAmount;

    @Schema(description = "发行总数量")
    private Integer total;

    @Schema(description = "已领取数量")
    private Integer used;

    @Schema(description = "生效时间")
    private LocalDateTime startTime;

    @Schema(description = "失效时间")
    private LocalDateTime endTime;

    @Schema(description = "状态 0-关闭 1-开启")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}