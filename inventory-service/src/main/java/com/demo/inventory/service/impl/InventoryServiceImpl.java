package com.demo.inventory.service.impl;

import com.demo.common.exception.BusinessException;
import com.demo.common.service.InventoryService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@DubboService
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final int INVALID_PARAMETER = 40001;
    private static final int PRODUCT_NOT_FOUND = 40002;
    private static final int STOCK_NOT_ENOUGH = 40003;

    private final Map<Long, AtomicInteger> stockMap = new ConcurrentHashMap<>();

    public InventoryServiceImpl() {
        stockMap.put(1L, new AtomicInteger(100));
        stockMap.put(2L, new AtomicInteger(50));
        stockMap.put(3L, new AtomicInteger(200));
    }

    @Override
    public int deduct(Long productId, int quantity) {
        validateProductId(productId);
        if (quantity <= 0) {
            throw new BusinessException(INVALID_PARAMETER, "扣减数量必须大于 0");
        }

        AtomicInteger stock = getRequiredStock(productId);
        while (true) {
            int current = stock.get();
            if (current < quantity) {
                throw new BusinessException(STOCK_NOT_ENOUGH,
                        "库存不足: productId=" + productId + ", current=" + current);
            }
            int remaining = current - quantity;
            if (stock.compareAndSet(current, remaining)) {
                return remaining;
            }
        }
    }

    public int getStock(Long productId) {
        validateProductId(productId);
        return getRequiredStock(productId).get();
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new BusinessException(INVALID_PARAMETER, "商品 ID 不能为空");
        }
    }

    private AtomicInteger getRequiredStock(Long productId) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) {
            throw new BusinessException(PRODUCT_NOT_FOUND, "商品不存在: " + productId);
        }
        return stock;
    }
}
