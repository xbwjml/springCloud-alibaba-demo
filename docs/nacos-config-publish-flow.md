# Nacos 配置发布流程

这张图描述的是：在 Nacos 控制台“编辑配置”页面点击“发布”之后，从控制台请求、服务端保存配置、发布变更事件，到客户端收到通知并拉取最新配置的一系列流程。

如果当前 Markdown 工具不支持 Mermaid，可以直接打开 SVG 图片版：

![Nacos 配置发布流程](./nacos-config-publish-flow.svg)

```mermaid
sequenceDiagram
    participant UI as Nacos 控制台页面
    participant Console as ConsoleConfigController
    participant Proxy as ConfigProxy
    participant Op as ConfigOperationService
    participant Store as ConfigInfoPersistService
    participant Pub as ConfigChangePublisher
    participant Notify as NotifyCenter
    participant Dump as DumpService
    participant Cache as ConfigCacheService
    participant RPC as RpcConfigChangeNotifier
    participant LP as LongPollingService
    participant Client as Nacos Client
    participant App as Spring 应用

    UI->>Console: POST 发布配置
    Console->>Console: validateWithContent<br/>参数校验、namespace 转换
    Console->>Proxy: publishConfig(configForm, requestInfo)
    Proxy->>Op: publishConfig(...)
    Op->>Store: insertOrUpdate / insertOrUpdateCas
    Store-->>Op: ConfigOperateResult(lastModified)
    Op->>Pub: notifyConfigChange(ConfigDataChangeEvent)
    Pub->>Notify: publishEvent(event)

    Notify->>Dump: 订阅 ConfigDataChangeEvent
    Dump->>Dump: 创建 DumpRequest
    Dump->>Cache: dump/update md5、本地磁盘缓存/JVM 缓存
    Cache->>Notify: publishEvent(LocalDataChangeEvent)

    Notify->>RPC: 订阅 LocalDataChangeEvent
    RPC->>Client: ConfigChangeNotifyRequest<br/>只通知 dataId/group/tenant 变了

    Notify->>LP: 订阅 LocalDataChangeEvent
    LP->>Client: 唤醒 HTTP 长轮询客户端

    Client->>Client: 标记 receiveNotifyChanged=true
    Client->>Client: executeConfigListen()
    Client->>Client: ConfigBatchListenRequest<br/>带本地 md5 检查
    Client->>Client: queryConfig 拉取最新内容
    Client->>App: 调用 Listener.receiveConfigInfo
    App->>App: Spring Cloud Alibaba 发布 RefreshEvent
```

## 关键源码入口

- `/Users/liminjie/Documents/myPrjs/nacos/console/src/main/java/com/alibaba/nacos/console/controller/v3/config/ConsoleConfigController.java`
- `/Users/liminjie/Documents/myPrjs/nacos/console/src/main/java/com/alibaba/nacos/console/proxy/config/ConfigProxy.java`
- `/Users/liminjie/Documents/myPrjs/nacos/config/src/main/java/com/alibaba/nacos/config/server/service/ConfigOperationService.java`
- `/Users/liminjie/Documents/myPrjs/nacos/config/src/main/java/com/alibaba/nacos/config/server/service/ConfigChangePublisher.java`
- `/Users/liminjie/Documents/myPrjs/nacos/config/src/main/java/com/alibaba/nacos/config/server/service/dump/DumpService.java`
- `/Users/liminjie/Documents/myPrjs/nacos/config/src/main/java/com/alibaba/nacos/config/server/service/ConfigCacheService.java`
- `/Users/liminjie/Documents/myPrjs/nacos/config/src/main/java/com/alibaba/nacos/config/server/remote/RpcConfigChangeNotifier.java`
- `/Users/liminjie/Documents/myPrjs/nacos/config/src/main/java/com/alibaba/nacos/config/server/service/LongPollingService.java`
- `/Users/liminjie/Documents/myPrjs/nacos/client/src/main/java/com/alibaba/nacos/client/config/impl/ClientWorker.java`
- `/Users/liminjie/Documents/myPrjs/nacos/client/src/main/java/com/alibaba/nacos/client/config/impl/CacheData.java`

## 核心结论

Nacos Server 推给客户端的是“配置已变更”的通知，不是完整配置内容。客户端收到通知后，会再通过查询配置接口拉取最新内容，然后触发本地 listener。
