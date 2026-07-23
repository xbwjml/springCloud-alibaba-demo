package com.demo.common.sentinel;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.demo.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Spring MVC 自动埋点资源被 Sentinel 拒绝时的统一 JSON 响应。
 */
@Component
public class SentinelWebBlockHandler implements BlockExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SentinelWebBlockHandler.class);

    private final ObjectMapper objectMapper;

    public SentinelWebBlockHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       BlockException exception) throws Exception {
        BlockResponse blockResponse = resolveResponse(exception);

        log.warn("[Sentinel] MVC 请求被拒绝: path={}, type={}",
                request.getRequestURI(), exception.getClass().getSimpleName());

        response.setStatus(blockResponse.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Result.fail(blockResponse.code(), blockResponse.message()));
    }

    private BlockResponse resolveResponse(BlockException exception) {
        if (exception instanceof AuthorityException) {
            return new BlockResponse(HttpStatus.FORBIDDEN, 403, "无权访问该资源");
        }
        if (exception instanceof DegradeException || exception instanceof SystemBlockException) {
            return new BlockResponse(HttpStatus.SERVICE_UNAVAILABLE, 503, "服务暂时不可用，请稍后重试");
        }
        if (exception instanceof FlowException) {
            return new BlockResponse(HttpStatus.TOO_MANY_REQUESTS, 429, "请求过于频繁，请稍后重试");
        }
        return new BlockResponse(HttpStatus.TOO_MANY_REQUESTS, 429, "请求被流量保护规则拒绝");
    }

    private record BlockResponse(HttpStatus status, int code, String message) {
    }
}
