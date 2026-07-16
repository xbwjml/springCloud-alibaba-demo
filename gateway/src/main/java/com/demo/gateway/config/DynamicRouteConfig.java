package com.demo.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 *   "id": "dynamic-order-service",
 *   "uri": "lb://order-service",
 *   "order": -1,
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
    private final ApplicationEventPublisher eventPublisher;
    private final Set<String> dynamicRouteIds = ConcurrentHashMap.newKeySet();

    private final AbstractListener routeListener = new AbstractListener() {
        @Override
        public void receiveConfigInfo(String configInfo) {
            log.info("[DynamicRoute] 检测到路由配置变更，刷新中...");
            refreshRoutes(configInfo);
        }
    };

    public DynamicRouteConfig(RouteDefinitionWriter routeDefinitionWriter,
                              NacosConfigManager nacosConfigManager,
                              ObjectMapper objectMapper,
                              ApplicationEventPublisher eventPublisher) {
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.nacosConfigManager = nacosConfigManager;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
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
            nacosConfigManager.getConfigService().addListener(ROUTE_DATA_ID, ROUTE_GROUP, routeListener);
            log.info("[DynamicRoute] 监听器已注册: {}/{}", ROUTE_GROUP, ROUTE_DATA_ID);

        } catch (Exception e) {
            log.error("[DynamicRoute] 初始化失败，回退到静态路由", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            nacosConfigManager.getConfigService().removeListener(ROUTE_DATA_ID, ROUTE_GROUP, routeListener);
        } catch (Exception e) {
            log.warn("[DynamicRoute] 移除监听器失败: {}/{}", ROUTE_GROUP, ROUTE_DATA_ID, e);
        }
    }

    /**
     * 解析 JSON 并注册路由到 Gateway
     */
    private synchronized void refreshRoutes(String configJson) {
        try {
            List<RouteDefinition> routes = configJson == null || configJson.isBlank()
                    ? List.of()
                    : objectMapper.readValue(configJson, new TypeReference<List<RouteDefinition>>() {});
            Set<String> newRouteIds = validateRoutes(routes);
            Set<String> oldRouteIds = Set.copyOf(dynamicRouteIds);

            Flux.fromIterable(oldRouteIds)
                    .concatMap(routeId -> routeDefinitionWriter.delete(Mono.just(routeId))
                            .onErrorResume(NotFoundException.class, e -> Mono.empty()))
                    .thenMany(Flux.fromIterable(routes)
                            .concatMap(route -> routeDefinitionWriter.save(Mono.just(route))))
                    .then()
                    .block(Duration.ofSeconds(10));

            dynamicRouteIds.clear();
            dynamicRouteIds.addAll(newRouteIds);
            eventPublisher.publishEvent(new RefreshRoutesEvent(this));

            for (RouteDefinition route : routes) {
                log.info("[DynamicRoute] 注册: id={}, uri={}, order={}",
                        route.getId(), route.getUri(), route.getOrder());
            }
            log.info("[DynamicRoute] 共加载 {} 条动态路由", routes.size());
        } catch (Exception e) {
            log.error("[DynamicRoute] 解析路由配置失败: {}", configJson, e);
        }
    }

    private Set<String> validateRoutes(List<RouteDefinition> routes) {
        Set<String> routeIds = new HashSet<>();
        for (RouteDefinition route : routes) {
            if (route.getId() == null || route.getId().isBlank()) {
                throw new IllegalArgumentException("动态路由 id 不能为空");
            }
            if (!routeIds.add(route.getId())) {
                throw new IllegalArgumentException("动态路由 id 重复: " + route.getId());
            }
        }
        return routeIds;
    }
}
