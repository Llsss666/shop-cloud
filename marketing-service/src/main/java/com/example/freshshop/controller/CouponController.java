package com.example.freshshop.controller;

import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Coupon;
import com.example.freshshop.service.CouponService;
import com.example.freshshop.utils.JwtUtil;
import com.example.freshshop.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "优惠券管理")
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    // ===================== 只注入 Service =====================
    @Autowired
    private CouponService couponService;

    // ===================== 管理员 =====================
    @Operation(summary = "【管理员】查询所有优惠券")
    @GetMapping("/admin/list")
    public Result<List<Coupon>> adminList() {
        return couponService.listAll(); // 调用 Service
    }

    @Operation(summary = "【管理员】新增优惠券")
    @PostMapping("/admin")
    public Result<Void> add(@RequestBody Coupon coupon) {
        couponService.save(coupon);
        return Result.success();
    }

    @Operation(summary = "【管理员】修改优惠券")
    @PutMapping("/admin")
    public Result<Void> update(@RequestBody Coupon coupon) {
        couponService.updateById(coupon);
        return Result.success();
    }

    @Operation(summary = "【管理员】删除优惠券")
    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.removeById(id);
        return Result.success();
    }

    // ===================== 用户 =====================
    @Operation(summary = "获取当前用户的优惠券列表")
    @GetMapping("/my")
    public Result<List<UserCouponVO>> myCoupons(
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return couponService.myCoupons(userId); // 调用 Service
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/receive/{couponId}")
    public Result<String> receive(
            @PathVariable Long couponId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        couponService.receive(couponId, userId); // 调用 Service
        return Result.success("领取成功");
    }

}