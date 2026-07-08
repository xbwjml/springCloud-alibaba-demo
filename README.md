# Spring Cloud Alibaba Demo

这是一个用于学习 Spring Cloud Alibaba 微服务体系的综合示例项目，覆盖 Nacos 注册发现与配置中心、Spring Cloud Gateway、OpenFeign、Dubbo RPC、Sentinel 网关限流等常见组件。

项目以“下单”为主线：

- gateway 作为统一入口，负责路由转发和简单鉴权
- order-service 作为订单服务，演示通过 Dubbo 和 OpenFeign 调用商品服务
- product-service 作为商品服务，提供 REST 接口和 Dubbo 服务
- inventory-service 作为库存服务，提供 REST 接口和 Dubbo 服务
- common 存放公共 DTO 和 Dubbo 接口定义

## 技术栈

- Java 21
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Spring Cloud Alibaba 2023.0.1.0
- Apache Dubbo 3.3.0
- Nacos
- Sentinel
- Spring Cloud Gateway
- OpenFeign
- Maven 多模块工程

## 项目结构

```text
springCloud-alibaba
├── common
│   └── 公共 DTO、Dubbo 服务接口
├── gateway
│   └── API 网关、路由、鉴权、Sentinel Gateway 配置
├── product-service
│   └── 商品服务，提供 REST 和 Dubbo 接口
├── inventory-service
│   └── 库存服务，提供 REST 和 Dubbo 接口
├── order-service
│   └── 订单服务，消费 Dubbo 服务和 OpenFeign 服务
└── pom.xml
```

## 模块说明

| 模块 | 端口 | 说明 |
| --- | ---: | --- |
| gateway | 8080 | 网关入口，转发 `/order/**`、`/product/**`、`/inventory/**` |
| product-service | 8081 | 商品服务，提供商品查询接口和 Dubbo 商品服务 |
| inventory-service | 8082 | 库存服务，提供库存查询、扣减接口和 Dubbo 库存服务 |
| order-service | 8083 | 订单服务，演示 Dubbo 下单和 Feign 下单 |
| common | - | 公共 DTO、Dubbo 接口定义 |

## 调用链路

### Dubbo 下单链路

```text
客户端
  -> gateway
  -> order-service
  -> product-service    通过 Dubbo 查询商品
  -> inventory-service  通过 Dubbo 扣减库存
```

### OpenFeign 下单链路

```text
客户端
  -> gateway
  -> order-service
  -> product-service    通过 OpenFeign 查询商品
  -> inventory-service  通过 Dubbo 扣减库存
```

## 前置依赖

本地需要先启动 Nacos。

默认配置：

- Nacos 地址：`127.0.0.1:8848`
- Nacos 用户名：`nacos`
- Nacos 密码：`nacos`

如果需要体验 Sentinel Gateway 限流，还需要额外启动 Sentinel Dashboard，并在配置中准备对应的规则数据。

## 编译项目

在项目根目录执行：

```bash
mvn clean package -DskipTests
```

## 启动顺序

建议按下面顺序启动，便于服务注册和依赖发现：

1. 启动 Nacos
2. 启动 `product-service`
3. 启动 `inventory-service`
4. 启动 `order-service`
5. 启动 `gateway`

可以分别进入模块启动：

```bash
mvn spring-boot:run -pl product-service
mvn spring-boot:run -pl inventory-service
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl gateway
```

也可以在 IDE 中分别运行以下启动类：

- `com.demo.product.ProductApplication`
- `com.demo.inventory.InventoryApplication`
- `com.demo.order.OrderApplication`
- `com.demo.gateway.GatewayApplication`

## 网关鉴权

gateway 中有一个演示用全局鉴权过滤器。

除白名单接口外，请求需要携带：

```http
Authorization: Bearer demo-token-2024
```

白名单路径：

- `/product/list`
- `/actuator`

## 接口测试

### 通过网关查看商品列表

```bash
curl http://localhost:8080/product/list
```

### 通过网关查询商品

```bash
curl \
  -H "Authorization: Bearer demo-token-2024" \
  http://localhost:8080/product/1
```

### 通过 Dubbo 创建订单

```bash
curl -X POST \
  -H "Authorization: Bearer demo-token-2024" \
  "http://localhost:8080/order/dubbo?productId=1&quantity=1"
```

### 通过 OpenFeign 创建订单

```bash
curl -X POST \
  -H "Authorization: Bearer demo-token-2024" \
  "http://localhost:8080/order/feign?productId=1&quantity=1"
```

### 查询库存

```bash
curl \
  -H "Authorization: Bearer demo-token-2024" \
  http://localhost:8080/inventory/stock/1
```

### 扣减库存

```bash
curl -X POST \
  -H "Authorization: Bearer demo-token-2024" \
  "http://localhost:8080/inventory/deduct?productId=1&quantity=1"
```

### 查看 Nacos 配置刷新演示

```bash
curl http://localhost:8083/config/demo
```

可以在 Nacos 中创建或修改 `order-service.yaml`：

```yaml
order:
  timeout: 5000
  feature:
    send-sms: false
```

再次访问 `/config/demo`，观察配置是否刷新。

## Nacos 配置说明

项目中各服务都配置了 Nacos Config：

- `gateway.yaml`
- `product-service.yaml`
- `inventory-service.yaml`
- `order-service.yaml`
- `common-config.yaml`

其中 gateway 还演示了通过 Nacos 动态加载路由配置：

- DataId：`gateway-dynamic-routes.json`
- Group：`DEFAULT_GROUP`

示例：

```json
[
  {
    "id": "order-service",
    "uri": "lb://order-service",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/order/**"
        }
      }
    ]
  }
]
```

## Sentinel Gateway 规则

gateway 支持从 Nacos 加载 Sentinel Gateway 规则。

路由限流规则：

- DataId：`sentinel-gateway-route-rules.json`
- Rule Type：`gw_flow`

API 分组规则：

- DataId：`sentinel-gateway-api-rules.json`
- Rule Type：`gw_api_group`

这些规则适合用于学习网关层限流、API 分组限流和规则持久化。

## 学习重点

这个项目适合按下面顺序阅读：

1. 先看 `common`，理解 Dubbo 接口和 DTO 如何被多个服务共享
2. 再看 `product-service` 和 `inventory-service`，理解服务提供者如何同时暴露 REST 和 Dubbo
3. 然后看 `order-service`，对比 Dubbo 调用和 OpenFeign 调用
4. 最后看 `gateway`，理解统一入口、服务发现路由、全局过滤器和 Sentinel Gateway

建议重点理解：

- Nacos Discovery 和 Dubbo Registry 都可以使用 Nacos，但服务模型不同
- OpenFeign 走 HTTP，Dubbo 走 RPC，二者适用场景不同
- Gateway 的 `lb://service-name` 依赖服务注册发现和负载均衡
- 配置中心的刷新需要 `spring.config.import` 和 `@RefreshScope` 配合
- Sentinel 规则可以通过 Nacos 持久化，避免应用重启后规则丢失

## 常见排查点

- 访问网关接口返回 401：检查是否携带 `Authorization` 请求头
- 服务调用失败：检查 Nacos 中服务是否已经注册
- Feign 调用失败：检查 `product-service` 是否启动，并且服务名是否为 `product-service`
- Dubbo 调用失败：检查 provider 是否启动，Dubbo 注册中心是否连上 Nacos
- 配置没有刷新：检查 Nacos DataId、Group、文件后缀和 `spring.config.import`
- Gateway 路由失败：检查路由 Path 是否匹配，以及下游服务是否已注册

## 适合继续扩展的方向

- 增加统一异常处理和统一响应结构
- 增加 OpenFeign fallback 或 Sentinel 降级示例
- 增加 Dubbo 超时、重试、版本号、分组示例
- 增加 Seata 分布式事务示例
- 增加 RocketMQ 异步消息示例
- 增加 Redis 缓存商品详情示例
- 增加 Docker Compose，一键启动 Nacos、Sentinel 和各服务
