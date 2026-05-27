package com.example.freshshop.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalesCategoryVO {
    private String categoryName; // 品类名称
    private BigDecimal totalSales; // 该品类销售额
    private Integer totalQuantity; // 该品类销量
    private BigDecimal proportion; // 占总销售额比例
}