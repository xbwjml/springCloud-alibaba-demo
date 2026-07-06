package com.demo.common.service;

import com.demo.common.dto.ProductDTO;

/**
 * Dubbo RPC 接口：商品服务
 * 由 product-service 提供，order-service 消费
 */
public interface ProductService {
    ProductDTO getById(Long id);
}
