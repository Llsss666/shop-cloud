package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
@Schema(description = "用户优惠券实体")
public class UserCoupon {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "状态 0-未使用 1-已使用 2-已过期")
    private Integer status;

    @Schema(description = "领取时间")
    private LocalDateTime createTime;
}