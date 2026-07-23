# Sentinel Nacos 规则样例

本目录中的 JSON 文件与各服务 `application.yml` 中的 DataId 一一对应，可复制到 Nacos 的 `DEFAULT_GROUP`。

这些阈值只用于本地演示，不代表生产容量。生产阈值应根据压测得到的吞吐量、P99 RT、线程池、连接池和下游容量确定，并预留安全余量。

规则职责：

- Gateway：按路由和 API 分组限制外部入口流量。
- product-service：保护商品查询，并演示商品 ID 热点参数限流。
- inventory-service：按并发线程数保护写操作；被拒绝时不会扣库存。
- order-service：保护订单入口，并对商品服务出站调用进行慢调用熔断。

发布时保持文件名作为 Nacos DataId，Group 使用 `DEFAULT_GROUP`。配置中心是规则事实来源；不要依赖 Dashboard 内存规则长期保存。

集群流控未在本项目启用。只有存在“所有实例共享严格总配额”的业务需求时，才应引入 Token Server/Client，并同时保留本地限流兜底。
