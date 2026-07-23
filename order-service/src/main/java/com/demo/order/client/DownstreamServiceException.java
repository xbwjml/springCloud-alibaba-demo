package com.demo.order.client;

/**
 * 下游系统故障。与可预期的业务异常分开，供 Sentinel 统计并触发熔断。
 */
class DownstreamServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DownstreamServiceException(String message) {
        super(message);
    }
}
