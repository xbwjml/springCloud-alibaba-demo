package com.demo.order.controller;

import com.demo.common.dto.OrderDTO;
import com.demo.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 通过 Dubbo RPC 下单
     */
    @PostMapping("/dubbo")
    public Map<String, Object> createViaDubbo(@RequestParam Long productId,
                                               @RequestParam(defaultValue = "1") int quantity) {
        OrderDTO order = orderService.createOrderViaDubbo(productId, quantity);
        return Map.of("success", true, "order", order, "method", "Dubbo RPC");
    }

    /**
     * 通过 OpenFeign HTTP 下单
     */
    @PostMapping("/feign")
    public Map<String, Object> createViaFeign(@RequestParam Long productId,
                                               @RequestParam(defaultValue = "1") int quantity) {
        OrderDTO order = orderService.createOrderViaFeign(productId, quantity);
        return Map.of("success", true, "order", order, "method", "OpenFeign HTTP");
    }
}
