package com.demo.order.client;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.demo.common.dto.ProductDTO;
import com.demo.common.exception.BusinessException;
import com.demo.common.result.Result;
import com.demo.common.sentinel.SentinelResources;
import com.demo.order.feign.ProductFeignClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductClientFacadeSentinelTest.TestConfiguration.class)
class ProductClientFacadeSentinelTest {

    @Autowired
    private ProductClientFacade productClientFacade;

    @Autowired
    private ProductFeignClient productFeignClient;

    @BeforeEach
    void resetFeignClient() {
        reset(productFeignClient);
    }

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void downstreamFailureUsesSafeFallback() {
        when(productFeignClient.getById(1L))
                .thenReturn(Result.fail(503, "商品服务暂时不可用"));

        assertThatThrownBy(() -> productClientFacade.getById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getCode()).isEqualTo(50311);
                });
    }

    @Test
    void businessFailureIsNotConvertedToSystemFallback() {
        when(productFeignClient.getById(999L))
                .thenReturn(Result.fail(40401, "商品不存在: 999"));

        assertThatThrownBy(() -> productClientFacade.getById(999L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo(40401);
                });
    }

    @Test
    void openFlowRuleStopsFeignCall() {
        FlowRule rule = new FlowRule(SentinelResources.ORDER_PRODUCT_QUERY)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(0);
        FlowRuleManager.loadRules(List.of(rule));

        assertThatThrownBy(() -> productClientFacade.getById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfiguration {

        @Bean
        ProductFeignClient productFeignClient() {
            return mock(ProductFeignClient.class);
        }

        @Bean
        ProductClientFacade productClientFacade(ProductFeignClient productFeignClient) {
            return new ProductClientFacade(productFeignClient);
        }

        @Bean
        SentinelResourceAspect sentinelResourceAspect() {
            return new SentinelResourceAspect();
        }
    }
}
