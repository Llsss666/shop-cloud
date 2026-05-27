package com.example.freshshop.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    private String roleName; // 新增角色名称字段
}