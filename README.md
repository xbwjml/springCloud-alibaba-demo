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
│   └── 公共 DTO、Dubbo 服务接口、统一响应和业务异常
├── gateway
│   └── API 网关、路由、鉴权、Sentinel Gateway 配置
├── product-service
│   └── 商品服务，提供 REST 和 Dubbo 接口
├── inventory-service
│   └── 库存服务，提供 REST 和 Dubbo 接口
├── order-service
│   └── 订单服务，消费 Dubbo 服务和 OpenFeign 服务
├── sentinel-rules
│   └── 可直接发布到 Nacos 的 Sentinel 规则样例
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

Sentinel Dashboard 地址为 `http://127.0.0.1:8090`。各应用连接 Dashboard 的命令端口固定如下，避免同一台机器启动多个服务时依赖自动递增端口：

| 应用 | Sentinel 命令端口 |
| --- | ---: |
| gateway | 8719 |
| product-service | 8720 |
| inventory-service | 8721 |
| order-service | 8722 |

Dashboard 只有在应用启动并产生过访问后才会显示对应应用；它用于观察和临时调试，规则事实来源是 Nacos。

## 编译项目

在项目根目录执行：

```bash
mvn clean package -DskipTests
```

运行完整自动化测试：

```bash
mvn test
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
  "http://localhost:8080/product/getById?id=1"
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
    "id": "dynamic-order-service",
    "uri": "lb://order-service",
    "order": -1,
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

动态路由应使用与静态路由不同的 ID，并将 `order` 设为 `-1`，使其优先于静态兜底路由。配置发布后，Gateway 会删除上一版动态路由、注册新路由并刷新路由缓存；发布空配置则会清除动态路由并退回静态路由。

## 统一响应与业务异常

REST 接口统一返回 `Result<T>`。可预期的业务错误使用 `BusinessException`，由 `GlobalExceptionHandler` 转换为对应 HTTP 状态和业务错误码；未处理的系统异常统一返回通用错误信息并记录完整日志。

库存扣减会校验商品 ID 和扣减数量，并使用 CAS 保证并发扣减时不会出现短暂的负库存。

## Sentinel 实现

项目按“入口限流、服务保护、出站熔断、规则持久化、统一兜底、可观测与测试”实现 Sentinel，而不是只连接 Dashboard。

### 稳定资源名

| 保护位置 | Sentinel 资源 | 作用 |
| --- | --- | --- |
| Gateway | `product-service`、`order-service` | 按网关路由限制外部流量 |
| Gateway | `product-query-api` | 按 `/product/**` API 分组限流 |
| 商品服务 | `product.queryById` | 商品查询限流、慢调用熔断、热点参数限流 |
| 库存服务 | `inventory.deduct` | 按并发线程数保护库存写操作 |
| 订单服务 | `/order/feign` | 保护 Feign 下单入口 |
| 订单服务出站 | `order.product.query` | 隔离商品服务调用并进行慢调用熔断 |

业务资源使用常量和 `@SentinelResource`，不把 Java 方法签名当作长期规则名。Gateway 关闭了普通 WebFlux URL 埋点，只保留路由和 API 分组资源，避免同一请求产生两套含义重叠的规则。

### Nacos 规则持久化

[`sentinel-rules`](./sentinel-rules) 中的 9 份 JSON 与各应用配置的 DataId 一一对应：

- Gateway：路由流控、API 分组
- product-service：流控、熔断、热点参数
- inventory-service：流控、熔断
- order-service：入口流控、出站熔断

在 Nacos 的 `DEFAULT_GROUP` 中，以文件名作为 DataId 发布文件内容即可。应用启动后会订阅规则变更，无需重启。样例阈值仅用于本地演示，生产阈值必须根据压测、P99、线程池/连接池和下游容量确定，并预留余量。

不要把 Dashboard 上临时修改的内存规则当成持久化方案；当前接入方向是 `Nacos -> 应用`，生产变更应在 Nacos 或配置发布平台完成。集群流控也没有默认开启，只有“所有实例共享严格总配额”时才值得引入 Token Server/Client。

### 限流、熔断与降级语义

- MVC 流控统一返回 HTTP 429；熔断和系统保护统一返回 HTTP 503；授权规则返回 HTTP 403。
- Gateway 返回同样结构的 JSON，不暴露异常堆栈或查询参数。
- 商品查询和库存扣减明确区分 `blockHandler` 与业务 `fallback`；业务异常不会被误判成系统故障。
- 库存写操作被限流时直接失败，不返回“假成功”，也不会发生库存扣减。
- Feign 先使用 1 秒连接超时、1.5 秒读取超时限制单次故障成本，再由 `order.product.query` 阻断持续故障流量。
- Feign fallback 只返回安全的 503 结果；订单服务在商品失败时不会继续扣库存。

### 可观测性

四个应用均启用了 Actuator 和 Prometheus：

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/sentinel
http://localhost:8080/actuator/prometheus
```

其他服务将端口替换为 `8081`、`8082`、`8083`。可用 `http.server.requests` 按应用、URI、HTTP 状态观察 429/503，并结合 Sentinel Dashboard 的通过 QPS、拒绝 QPS、RT 和线程数定位问题。生产环境应通过网络策略或 Spring Security 限制 Actuator 访问，并为拒绝率、熔断次数、P99、错误率设置告警。

### 自动化验证

测试覆盖了统一限流响应、业务异常不误降级、库存限流不扣减、Feign 失败不扣库存、超时配置，以及熔断器从打开到半开探测再恢复的过程。

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
- Dashboard 看不到服务：先确认应用连接的是 `8090`，再访问一次业务接口，并检查 8719～8722 命令端口
- Sentinel 规则不生效：检查 Nacos DataId、`DEFAULT_GROUP`、JSON 格式和 `rule-type`
- Dubbo 调用失败：检查 provider 是否启动，Dubbo 注册中心是否连上 Nacos
- 配置没有刷新：检查 Nacos DataId、Group、文件后缀和 `spring.config.import`
- Gateway 路由失败：检查路由 Path 是否匹配，以及下游服务是否已注册

## 适合继续扩展的方向

- 将 Sentinel 规则发布纳入审核、灰度和回滚流程
- 接入 Grafana，并为 429、503、P99 和错误率建立仪表盘与告警
- 增加 Dubbo 超时、重试、版本号、分组示例
- 增加 Seata 分布式事务示例
- 增加 RocketMQ 异步消息示例
- 增加 Redis 缓存商品详情示例
- 增加 Docker Compose，一键启动 Nacos、Sentinel 和各服务
