package com.example.freshshop.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserRoleBatchDTO {
    private Long userId;
    private List<Long> roleIds;
}