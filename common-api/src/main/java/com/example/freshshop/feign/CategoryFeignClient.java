package com.example.freshshop.feign;

import com.example.freshshop.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", contextId = "categoryFeign")
public interface CategoryFeignClient {

    /**
     * 根据分类ID获取分类名称
     */
    @GetMapping("/feign/category/getName")
    Result<String> getCategoryName(@RequestParam("categoryId") Long categoryId);
}