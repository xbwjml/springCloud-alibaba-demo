package com.demo.product.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.demo.common.dto.ProductDTO;
import com.demo.common.exception.BusinessException;
import com.demo.common.sentinel.SentinelResources;
import com.demo.common.service.ProductService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@DubboService
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final int PRODUCT_NOT_FOUND = 40401;
    private static final int PRODUCT_QUERY_BLOCKED = 42901;
    private static final int PRODUCT_SERVICE_UNAVAILABLE = 50301;

    private static final Map<Long, ProductDTO> PRODUCTS = Map.of(
            1L, ProductDTO.builder().id(1L).name("iPhone 16 Pro").price(new BigDecimal("8999.00")).stock(100).build(),
            2L, ProductDTO.builder().id(2L).name("MacBook Pro M4").price(new BigDecimal("14999.00")).stock(50).build(),
            3L, ProductDTO.builder().id(3L).name("AirPods Pro 3").price(new BigDecimal("1899.00")).stock(200).build()
    );

    @Override
    @SentinelResource(
            value = SentinelResources.PRODUCT_QUERY_BY_ID,
            blockHandler = "queryByIdBlocked",
            fallback = "queryByIdFallback",
            exceptionsToIgnore = BusinessException.class
    )
    public ProductDTO getById(Long id) {
        ProductDTO product = PRODUCTS.get(id);
        if (product == null) {
            throw new BusinessException(PRODUCT_NOT_FOUND, "商品不存在: " + id);
        }
        return product;
    }

    public ProductDTO queryByIdBlocked(Long id, BlockException exception) {
        if (exception instanceof DegradeException) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    PRODUCT_SERVICE_UNAVAILABLE, "商品服务正在熔断，请稍后重试");
        }
        throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                PRODUCT_QUERY_BLOCKED, "商品查询过于频繁，请稍后重试");
    }

    public ProductDTO queryByIdFallback(Long id, Throwable throwable) {
        log.error("商品查询发生非业务异常: id={}", id, throwable);
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                PRODUCT_SERVICE_UNAVAILABLE, "商品服务暂时不可用，请稍后重试");
    }
}
