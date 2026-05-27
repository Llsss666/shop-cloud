package com.example.freshshop.vo;

import com.example.freshshop.entity.Order;
import com.example.freshshop.entity.OrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderVO extends Order {
    private List<OrderItem> itemList;
}