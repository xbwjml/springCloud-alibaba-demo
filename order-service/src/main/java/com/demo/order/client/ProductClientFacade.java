package com.demo.order.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.demo.common.dto.ProductDTO;
import com.demo.common.exception.BusinessException;
import com.demo.common.result.Result;
import com.demo.common.sentinel.SentinelResources;
import com.demo.order.feign.ProductFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 订单服务访问商品系统的稳定边界。
 *
 * Feign 负责 HTTP 调用，Facade 负责统一校验响应并提供稳定的 Sentinel 资源名。
 */
@Service
public class ProductClientFacade {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFacade.class);
    private static final int PRODUCT_QUERY_BLOCKED = 42911;
    private static final int PRODUCT_SERVICE_UNAVAILABLE = 50311;

    private final ProductFeignClient productFeignClient;

    public ProductClientFacade(ProductFeignClient productFeignClient) {
        this.productFeignClient = productFeignClient;
    }

    @SentinelResource(
            value = SentinelResources.ORDER_PRODUCT_QUERY,
            blockHandler = "queryProductBlocked",
            fallback = "queryProductFallback",
            exceptionsToIgnore = BusinessException.class
    )
    public ProductDTO getById(Long productId) {
        Result<ProductDTO> result = productFeignClient.getById(productId);
        if (result == null) {
            throw new DownstreamServiceException("商品服务未返回结果");
        }
        if (!result.isSuccess()) {
            if (isHttpServerError(result.getCode())) {
                throw new DownstreamServiceException(defaultMessage(result.getMessage()));
            }
            throw new BusinessException(result.getCode(), defaultMessage(result.getMessage()));
        }
        if (result.getData() == null) {
            throw new DownstreamServiceException("商品服务返回数据为空");
        }
        return result.getData();
    }

    public ProductDTO queryProductBlocked(Long productId, BlockException exception) {
        if (exception instanceof DegradeException) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    PRODUCT_SERVICE_UNAVAILABLE, "商品服务调用正在熔断，请稍后重试");
        }
        throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                PRODUCT_QUERY_BLOCKED, "商品服务调用过于频繁，请稍后重试");
    }

    public ProductDTO queryProductFallback(Long productId, Throwable throwable) {
        log.warn("商品服务调用降级: productId={}, type={}, message={}",
                productId, throwable.getClass().getSimpleName(), throwable.getMessage());
        log.debug("商品服务调用降级详情", throwable);
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                PRODUCT_SERVICE_UNAVAILABLE, "商品服务暂时不可用，请稍后重试");
    }

    private String defaultMessage(String message) {
        return message == null || message.isBlank() ? "商品服务调用失败" : message;
    }

    private boolean isHttpServerError(int code) {
        return code >= 500 && code <= 599;
    }
}
