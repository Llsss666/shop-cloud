package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Address;
import com.example.freshshop.service.AddressService;
import com.example.freshshop.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "收货地址管理") // 分组名称
@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Operation(summary = "获取当前用户的收货地址列表")
    @GetMapping("/list")
    public Result<List<Address>> list(
            @Parameter(description = "登录令牌", required = true)
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return addressService.getList(userId);
    }

    @Operation(summary = "新增/修改收货地址")
    @PostMapping("/save")
    public Result<Void> save(
            @Parameter(description = "地址实体", required = true)
            @RequestBody Address address,

            @Parameter(description = "登录令牌", required = true)
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return addressService.addOrUpdate(address, userId);
    }

    @Operation(summary = "设置默认收货地址")
    @PutMapping("/default/{id}")
    public Result<Void> setDefault(
            @Parameter(description = "地址ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "登录令牌", required = true)
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return addressService.setDefault(id, userId);
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "地址ID", required = true)
            @PathVariable Long id
    ) {
        addressService.removeById(id);
        return Result.success();
    }
}