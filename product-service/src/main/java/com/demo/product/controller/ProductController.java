package com.demo.product.controller;

import com.demo.common.dto.ProductDTO;
import com.demo.product.service.impl.ProductServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductServiceImpl productService;

    public ProductController(ProductServiceImpl productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ProductDTO getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/list")
    public List<ProductDTO> list() {
        return List.of(
                ProductDTO.builder().id(1L).name("iPhone 16 Pro").price(new BigDecimal("8999.00")).stock(100).build(),
                ProductDTO.builder().id(2L).name("MacBook Pro M4").price(new BigDecimal("14999.00")).stock(50).build(),
                ProductDTO.builder().id(3L).name("AirPods Pro 3").price(new BigDecimal("1899.00")).stock(200).build()
        );
    }
}
