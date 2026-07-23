package com.demo.product.service.impl;

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
@ContextConfiguration(classes = ProductServiceSentinelTest.TestConfiguration.class)
class ProductServiceSentinelTest {

    @Autowired
    private ProductServiceImpl productService;

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void flowRuleUsesBlockHandler() {
        FlowRule rule = new FlowRule(SentinelResources.PRODUCT_QUERY_BY_ID)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(0);
        FlowRuleManager.loadRules(List.of(rule));

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getCode()).isEqualTo(42901);
                });
    }

    @Test
    void businessErrorDoesNotUseSystemFallback() {
        assertThatThrownBy(() -> productService.getById(999L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo(40401);
                });
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfiguration {

        @Bean
        ProductServiceImpl productService() {
            return new ProductServiceImpl();
        }

        @Bean
        SentinelResourceAspect sentinelResourceAspect() {
            return new SentinelResourceAspect();
        }
    }
}
