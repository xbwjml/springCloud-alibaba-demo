package com.demo.order.service;

import com.demo.common.dto.OrderDTO;
import com.demo.common.dto.ProductDTO;
import com.demo.common.result.Result;
import com.demo.common.service.InventoryService;
import com.demo.common.service.ProductService;
import com.demo.order.feign.ProductFeignClient;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务 - 同时演示 Dubbo 和 OpenFeign 两种调用方式
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // ========== Dubbo RPC 调用 ==========
    @DubboReference(check = false, timeout = 3000, retries = 0)
    private ProductService productService;       // Dubbo 查商品

    @DubboReference(check = false, timeout = 3000, retries = 0)
    private InventoryService inventoryService;   // Dubbo 扣库存

    // ========== OpenFeign HTTP 调用 ==========
    private final ProductFeignClient productFeignClient;

    private final AtomicLong orderIdGenerator = new AtomicLong(1);

    public OrderService(ProductFeignClient productFeignClient) {
        this.productFeignClient = productFeignClient;
    }

    /**
     * 通过 Dubbo RPC 下单（高性能，推荐内部调用）
     */
    public OrderDTO createOrderViaDubbo(Long productId, int quantity) {
        log.info("[Dubbo] 开始下单: productId={}, quantity={}", productId, quantity);

        ProductDTO product = productService.getById(productId);
        inventoryService.deduct(productId, quantity);

        return buildOrder(product, quantity);
    }

    /**
     * 通过 OpenFeign HTTP 下单（演示跨协议调用，适合外部系统对接）
     */
    public OrderDTO createOrderViaFeign(Long productId, int quantity) {
        log.info("[Feign] 开始下单: productId={}, quantity={}", productId, quantity);

        Result<ProductDTO> productResult = productFeignClient.getById(productId);
        ProductDTO product = productResult.getData();
        inventoryService.deduct(productId, quantity);

        return buildOrder(product, quantity);
    }

    private OrderDTO buildOrder(ProductDTO product, int quantity) {
        return OrderDTO.builder()
                .id(orderIdGenerator.getAndIncrement())
                .productId(product.getId())
                .quantity(quantity)
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status("CREATED")
                .createTime(LocalDateTime.now())
                .build();
    }
}
