package com.example.freshshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户登录请求参数")
public class UserLoginDTO {

    @Schema(description = "用户名", required = true)
    private String username;

    @Schema(description = "密码", required = true)
    private String password;
}