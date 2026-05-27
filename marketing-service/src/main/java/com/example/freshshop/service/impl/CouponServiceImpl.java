package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Coupon;
import com.example.freshshop.entity.UserCoupon;
import com.example.freshshop.mapper.CouponMapper;
import com.example.freshshop.mapper.UserCouponMapper;
import com.example.freshshop.service.CouponService;
import com.example.freshshop.vo.UserCouponVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Override
    public Result<List<Coupon>> listAll() {
        return Result.success(list());
    }

    @Override
    @Transactional
    public Result<Void> receive(Long couponId, Long userId) {
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            return Result.error("优惠券不存在");
        }
        if (coupon.getTotal() <= coupon.getUsed()) {
            return Result.error("优惠券已领完");
        }

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId);
        if (userCouponMapper.exists(wrapper)) {
            return Result.error("你已领取过该优惠券");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCouponMapper.insert(userCoupon);

        coupon.setUsed(coupon.getUsed() + 1);
        updateById(coupon);

        return Result.success();
    }

    // =============================
    // 🔥 修复：永远不会返回 null
    // =============================
    @Override
    public Result<List<UserCouponVO>> myCoupons(Long userId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);

        List<UserCoupon> list = userCouponMapper.selectList(wrapper);

        // 空数据时返回空数组，不是 null
        if (list == null || list.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<UserCouponVO> voList = list.stream().map(uc -> {
            UserCouponVO vo = new UserCouponVO();
            vo.setId(uc.getId());
            vo.setCouponId(uc.getCouponId());
            vo.setStatus(uc.getStatus());
            vo.setCreateTime(uc.getCreateTime());

            Coupon coupon = getById(uc.getCouponId());
            if (coupon != null) {
                vo.setName(coupon.getName());
                vo.setValue(coupon.getValue());
                vo.setMinAmount(coupon.getMinAmount());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }
}