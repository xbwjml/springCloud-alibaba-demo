package com.demo.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 动态路由配置
 *
 * 从 Nacos 配置中心读取路由规则 JSON，动态注册到 Gateway
 * 修改 Nacos 中的路由配置后，无需重启 Gateway 即可生效
 *
 * Nacos 中配置：
 *   DataId: gateway-dynamic-routes.json
 *   Group:   DEFAULT_GROUP
 *
 * JSON 格式示例：
 * [{
 *   "id": "order-service",
 *   "uri": "lb://order-service",
 *   "predicates": [{"name": "Path", "args": {"pattern": "/order/**"}}],
 *   "filters": [{"name": "StripPrefix", "args": {"parts": "0"}}]
 * }]
 */
@Configuration
public class DynamicRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicRouteConfig.class);

    private static final String ROUTE_DATA_ID = "gateway-dynamic-routes.json";
    private static final String ROUTE_GROUP = "DEFAULT_GROUP";

    private final RouteDefinitionWriter routeDefinitionWriter;
    private final NacosConfigManager nacosConfigManager;
    private final ObjectMapper objectMapper;

    public DynamicRouteConfig(RouteDefinitionWriter routeDefinitionWriter,
                              NacosConfigManager nacosConfigManager,
                              ObjectMapper objectMapper) {
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.nacosConfigManager = nacosConfigManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动时从 Nacos 拉取路由配置，并注册监听器
     */
    @PostConstruct
    public void init() {
        try {
            // 首次加载
            String config = nacosConfigManager.getConfigService()
                    .getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            if (config != null && !config.isEmpty()) {
                refreshRoutes(config);
            } else {
                log.info("[DynamicRoute] Nacos 暂无动态路由配置 ({}), 使用静态路由", ROUTE_DATA_ID);
            }

            // 注册监听：Nacos 配置变更 → 自动刷新路由
            nacosConfigManager.getConfigService().addListener(ROUTE_DATA_ID, ROUTE_GROUP, new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("[DynamicRoute] 检测到路由配置变更，刷新中...");
                    refreshRoutes(configInfo);
                }
            });
            log.info("[DynamicRoute] 监听器已注册: {}/{}", ROUTE_GROUP, ROUTE_DATA_ID);

        } catch (Exception e) {
            log.error("[DynamicRoute] 初始化失败，回退到静态路由", e);
        }
    }

    /**
     * 解析 JSON 并注册路由到 Gateway
     */
    private void refreshRoutes(String configJson) {
        try {
            List<RouteDefinition> routes = objectMapper.readValue(configJson,
                    new TypeReference<List<RouteDefinition>>() {});
            for (RouteDefinition route : routes) {
                routeDefinitionWriter.save(Mono.just(route)).subscribe();
                log.info("[DynamicRoute] 注册: id={}, uri={}", route.getId(), route.getUri());
            }
            log.info("[DynamicRoute] 共加载 {} 条动态路由", routes.size());
        } catch (Exception e) {
            log.error("[DynamicRoute] 解析路由配置失败: {}", configJson, e);
        }
    }
}
