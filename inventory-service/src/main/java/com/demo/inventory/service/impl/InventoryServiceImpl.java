package com.demo.inventory.service.impl;

import com.demo.common.service.InventoryService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@DubboService
@Service
public class InventoryServiceImpl implements InventoryService {

    private final Map<Long, AtomicInteger> stockMap = new ConcurrentHashMap<>();

    public InventoryServiceImpl() {
        stockMap.put(1L, new AtomicInteger(100));
        stockMap.put(2L, new AtomicInteger(50));
        stockMap.put(3L, new AtomicInteger(200));
    }

    @Override
    public int deduct(Long productId, int quantity) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        int remaining = stock.addAndGet(-quantity);
        if (remaining < 0) {
            stock.addAndGet(quantity); // 回滚
            throw new RuntimeException("库存不足: " + productId);
        }
        return remaining;
    }

    public int getStock(Long productId) {
        AtomicInteger stock = stockMap.get(productId);
        return stock != null ? stock.get() : 0;
    }
}
