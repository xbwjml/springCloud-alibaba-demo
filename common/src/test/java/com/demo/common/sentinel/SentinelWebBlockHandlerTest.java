package com.demo.common.sentinel;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelWebBlockHandlerTest {

    private ObjectMapper objectMapper;
    private SentinelWebBlockHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new SentinelWebBlockHandler(objectMapper);
    }

    @Test
    void flowControlReturns429Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/product/getById");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new FlowException("/product/getById"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(body.get("code").asInt()).isEqualTo(429);
        assertThat(body.get("message").asText()).contains("请求过于频繁");
    }

    @Test
    void circuitBreakerReturns503Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/product/getById");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new DegradeException("/product/getById"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(body.get("code").asInt()).isEqualTo(503);
    }
}
