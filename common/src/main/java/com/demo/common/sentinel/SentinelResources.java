package com.demo.common.sentinel;

/**
 * 跨代码、测试和规则配置保持稳定的 Sentinel 资源名。
 */
public final class SentinelResources {

    public static final String PRODUCT_QUERY_BY_ID = "product.queryById";
    public static final String INVENTORY_DEDUCT = "inventory.deduct";
    public static final String ORDER_PRODUCT_QUERY = "order.product.query";

    private SentinelResources() {
    }
}
