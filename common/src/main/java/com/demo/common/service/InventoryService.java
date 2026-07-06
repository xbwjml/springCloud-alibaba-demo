package com.demo.common.service;

/**
 * Dubbo RPC 接口：库存服务
 * 由 inventory-service 提供，order-service 消费
 */
public interface InventoryService {
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 扣减后剩余库存
     */
    int deduct(Long productId, int quantity);
}
