package com.demo.order.feign;

import com.demo.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 调用失败时的统一兜底工厂。
 */
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignFallbackFactory.class);
    private static final int SERVICE_UNAVAILABLE = 503;

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.warn("调用 product-service 失败，执行 Feign 兜底: type={}, message={}",
                cause.getClass().getSimpleName(), cause.getMessage());
        log.debug("product-service Feign 失败详情", cause);
        return id -> Result.fail(SERVICE_UNAVAILABLE, "商品服务暂时不可用，请稍后重试");
    }
}
