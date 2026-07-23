package com.demo.inventory.service.impl;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.demo.common.exception.BusinessException;
import com.demo.common.sentinel.SentinelResources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InventoryServiceSentinelTest.TestConfiguration.class)
class InventoryServiceSentinelTest {

    @Autowired
    private InventoryServiceImpl inventoryService;

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void blockedDeductionHasNoInventorySideEffect() {
        int stockBefore = inventoryService.getStock(1L);
        FlowRule rule = new FlowRule(SentinelResources.INVENTORY_DEDUCT)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(0);
        FlowRuleManager.loadRules(List.of(rule));

        assertThatThrownBy(() -> inventoryService.deduct(1L, 1))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getCode()).isEqualTo(42902);
                });
        assertThat(inventoryService.getStock(1L)).isEqualTo(stockBefore);
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfiguration {

        @Bean
        InventoryServiceImpl inventoryService() {
            return new InventoryServiceImpl();
        }

        @Bean
        SentinelResourceAspect sentinelResourceAspect() {
            return new SentinelResourceAspect();
        }
    }
}
