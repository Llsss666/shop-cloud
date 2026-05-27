package com.example.freshshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("address")
@Schema(description = "收货地址实体")
public class Address {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "收货人")
    private String consignee;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "收货地址")
    private String address;

    @Schema(description = "是否默认地址 0-否 1-是")
    private Integer isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}