package com.demo.order.feign;

import com.demo.common.dto.ProductDTO;
import com.demo.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign 客户端：通过 HTTP 调用 product-service 的 REST 接口
 * 演示与 Dubbo 并存的场景（例如跨语言调用、外部 API）
 * 实际生产中对内部微服务优先用 Dubbo，Feign 常用于对接外部系统
 */
@FeignClient(
        name = "product-service",
        path = "/product",
        fallbackFactory = ProductFeignFallbackFactory.class
)
public interface ProductFeignClient {

    @GetMapping("/getById")
    Result<ProductDTO> getById(@RequestParam("id") Long id);
}
