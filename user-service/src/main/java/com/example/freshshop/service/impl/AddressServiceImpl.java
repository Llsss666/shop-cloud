package com.example.freshshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Address;
import com.example.freshshop.mapper.AddressMapper;
import com.example.freshshop.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public Result<List<Address>> getList(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId);
        wrapper.orderByDesc(Address::getIsDefault);
        return Result.success(list(wrapper));
    }

    @Override
    @Transactional
    public Result<Void> addOrUpdate(Address address, Long userId) {
        address.setUserId(userId);
        saveOrUpdate(address);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> setDefault(Long id, Long userId) {
        // 取消所有默认
        update(new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));

        // 设置新默认
        update(new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, id)
                .set(Address::getIsDefault, 1));
        return Result.success();
    }
}