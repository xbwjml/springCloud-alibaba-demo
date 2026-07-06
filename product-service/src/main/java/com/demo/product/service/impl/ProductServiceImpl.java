package com.demo.product.service.impl;

import com.demo.common.dto.ProductDTO;
import com.demo.common.service.ProductService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@DubboService
@Service
public class ProductServiceImpl implements ProductService {

    private static final Map<Long, ProductDTO> PRODUCTS = Map.of(
            1L, ProductDTO.builder().id(1L).name("iPhone 16 Pro").price(new BigDecimal("8999.00")).stock(100).build(),
            2L, ProductDTO.builder().id(2L).name("MacBook Pro M4").price(new BigDecimal("14999.00")).stock(50).build(),
            3L, ProductDTO.builder().id(3L).name("AirPods Pro 3").price(new BigDecimal("1899.00")).stock(200).build()
    );

    @Override
    public ProductDTO getById(Long id) {
        ProductDTO product = PRODUCTS.get(id);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + id);
        }
        return product;
    }
}
