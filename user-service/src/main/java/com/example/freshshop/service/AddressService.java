package com.example.freshshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.freshshop.common.Result;
import com.example.freshshop.entity.Address;
import java.util.List;

public interface AddressService extends IService<Address> {
    Result<List<Address>> getList(Long userId);
    Result<Void> addOrUpdate(Address address, Long userId);
    Result<Void> setDefault(Long id, Long userId);
}