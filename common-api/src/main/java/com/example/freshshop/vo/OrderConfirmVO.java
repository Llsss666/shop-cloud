package com.example.freshshop.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderConfirmVO {
    private Long orderId;
    private BigDecimal totalAmount;
    private String address;
    private String consignee;
    private String phone;
}