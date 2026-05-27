package com.example.freshshop.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalesDailyVO {
    private String date;          // 日期，格式：yyyy-MM-dd
    private BigDecimal totalSales; // 当日销售额
    private Integer orderCount;   // 当日订单数
    private Integer totalQuantity; // 当日商品总销量
}