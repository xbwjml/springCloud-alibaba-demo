package com.demo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 全局鉴权过滤器
 * 模拟 JWT 校验——从 Authorization 头中提取 token 并验证
 * 白名单路径直接放行，其余必须带有效 token
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** 白名单路径：不需要鉴权 */
    private static final List<String> WHITE_LIST = List.of(
            "/product/list",
            "/actuator"
    );

    /** 演示用硬编码 token，生产环境应调用认证中心校验 */
    private static final String VALID_TOKEN = "Bearer demo-token-2024";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 提取 Authorization 头
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("[Auth] 缺少 Authorization 头: {} {}", exchange.getRequest().getMethod(), path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 校验 token
        if (!VALID_TOKEN.equals(authHeader)) {
            log.warn("[Auth] Token 校验失败: {} {}", exchange.getRequest().getMethod(), path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 将用户信息写入 Header 透传给下游微服务
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", "demo-user-001")
                .header("X-User-Name", "DemoUser")
                .build();

        log.debug("[Auth] 鉴权通过: {} {}", exchange.getRequest().getMethod(), path);
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(item -> path.equals(item) || path.startsWith(item + "/"));
    }

    /** 值越小优先级越高，鉴权要最先执行 */
    @Override
    public int getOrder() {
        return -100;
    }
}
