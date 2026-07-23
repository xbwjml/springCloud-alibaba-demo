package com.demo.order.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SentinelCircuitBreakerRecoveryTest {

    private static final String RESOURCE = "test.downstream.recovery";

    @AfterEach
    void clearRules() {
        DegradeRuleManager.loadRules(List.of());
    }

    @Test
    void circuitBreakerOpensAndRecoversThroughHalfOpenProbe() throws Exception {
        DegradeRule rule = new DegradeRule(RESOURCE)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT)
                .setCount(1)
                .setMinRequestAmount(1)
                .setStatIntervalMs(1000)
                .setTimeWindow(1);
        DegradeRuleManager.loadRules(List.of(rule));

        recordFailure();
        recordFailure();

        assertThrows(DegradeException.class, () -> SphU.entry(RESOURCE));

        Thread.sleep(1100);
        assertDoesNotThrow(() -> {
            try (Entry ignored = SphU.entry(RESOURCE)) {
                // 半开探测成功，熔断器应恢复关闭状态。
            }
        });
        assertDoesNotThrow(() -> {
            try (Entry ignored = SphU.entry(RESOURCE)) {
                // 恢复后后续请求继续通过。
            }
        });
    }

    private void recordFailure() throws Exception {
        try (Entry entry = SphU.entry(RESOURCE)) {
            Tracer.traceEntry(new IllegalStateException("downstream failed"), entry);
        }
    }
}
