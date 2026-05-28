package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
@Schema(description = "订单实体")
public class Order {

    @Schema(description = "订单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "收货人")
    private String consignee;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "收货地址")
    private String address;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态 -1=取消 0=待支付 1=待发货 2=待收货 3=已完成")
    private Integer status;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货时间")
    private LocalDateTime sendTime;

    @Schema(description = "收货时间")
    private LocalDateTime receiveTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}