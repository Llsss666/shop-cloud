package com.example.freshshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "收货地址VO")
public class AddressVO {

    @Schema(description = "收货人")
    private String consignee;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "是否默认地址 0-否 1-是")
    private Integer isDefault;
}