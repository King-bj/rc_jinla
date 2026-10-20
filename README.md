# Notify Hub

统一通知中枢 MVP：业务系统通过 HTTP 提交通知，系统落库后按供应商模板异步投递，支持幂等、重试与死信。供应商地址和报文在页面上配置，改完不用重启。

详细设计见 [设计说明.md](设计说明.md)。当前运行路径是下面的接入、模板和调度。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 构建 | Maven 3.9+ |
| 框架 | Spring Boot 3.3 |
| 页面 | Thymeleaf |
| 持久化 | Spring Data JPA + MySQL 8 |
| HTTP 客户端 | JDK `HttpClient` |
| 调度 | Spring `@Scheduled`（本地消息表轮询投递） |

## 项目结构

```
src/main/java/com/notifyhub/
├── api/              # REST 接入：POST /api/v1/notifications
├── web/              # 供应商、任务、手工提交页面，以及本机 /mock/**
├── service/          # 提交、幂等、供应商配置与种子数据
├── dispatcher/       # 异步投递与重试
├── acl/              # 模板渲染与 HTTP 发送
├── domain/           # 领域模型
├── entity/           # JPA 实体（任务、供应商）
├── repository/       # 数据访问
└── config/           # 配置
```

## 环境要求

- **JDK 21**（必须；Spring Boot 3.x 不支持 Java 8/11）
- Maven 3.9+
- MySQL 8.0+

确认环境：

```bash
java -version    # 应显示 21.x
mvn -version     # Java version 也应为 21
```

如本机默认 Java 版本较低，请先设置 `JAVA_HOME` 指向 JDK 21。

## 快速开始

### 1. 创建数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

或手动执行：

```sql
CREATE DATABASE notify_hub DEFAULT CHARACTER SET utf8mb4;
```

表结构由 JPA `ddl-auto: update` 在启动时补齐。

### 2. 修改数据库连接

编辑 `src/main/resources/application.yml` 中的 `spring.datasource`。

### 3. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/notify-hub-0.1.0-SNAPSHOT.jar
```

开发模式：

```bash
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`。

### 4. 页面

| 地址 | 作用 |
|------|------|
| `/vendors` | 供应商列表。编码进入编辑页 |
| `/vendors/new` | 新建供应商。地址默认 `http://127.0.0.1:8080/mock/demo` |
| `/tasks` | 任务列表。可按状态、供应商、请求号筛选 |
| `/tasks/{id}` | 任务详情。待投递和失败重试时每 3 秒刷新；死信可重放 |
| `/notifications/new` | 手工提交。打开时带好请求号和示例报文，受理后可进入任务 |

供应商表为空时，启动会写入 CRM、AD、INVENTORY 三条模板，地址指向本机模拟接口。表里已有数据后不再用 yml 覆盖模板；仍以 `http://localhost:18081`、`18082`、`18083` 开头的地址会改写到当前 `channels.*.base-url`，其它地址不动。

### 5. 提交通知

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "req-001",
    "targetSystem": "CRM",
    "eventType": "PAYMENT_SUCCESS",
    "priority": 1,
    "payload": {
      "userId": "1001",
      "status": "PAID"
    }
  }'
```

响应示例（HTTP 202）：

```json
{
  "taskId": 1,
  "requestId": "req-001",
  "status": "PENDING",
  "message": "ACCEPTED"
}
```

`targetSystem` 填已启用的供应商编码。种子数据是 `CRM`、`AD`、`INVENTORY`。相同 `requestId` 重复提交会返回已有任务。

提交后状态是 `PENDING`。调度器默认每 10 秒投递一次，打到本机 `/mock/**` 会得到 2xx，任务变为 `SUCCESS`。

### 6. 本机模拟接口

`POST`、`PUT`、`PATCH` `/mock/**` 都返回：

```json
{"accepted": true}
```

种子路径为：

- `POST /mock/crm/contacts/update`
- `POST /mock/ad/conversions`
- `POST /mock/inventory/stock/adjust`

任意 `/mock/` 下的路径都可以，用来在本机看一次成功投递。

## 供应商模板

地址、请求头、报文体存在 `notification_vendor`。占位符：

| 占位符 | 含义 |
|--------|------|
| `{{eventType}}`、`{{requestId}}` | 按字符串插入，模板里自己写引号 |
| `{{payload}}` | 插入整段 JSON |
| `{{payload.字段}}` | 插入该字段的 JSON 值 |

请求头是 JSON 对象，留空则使用 `Content-Type: application/json`。提交报文里的 headers 会覆盖同名模板请求头。渲染后的 body 必须是合法 JSON。页面上的「预览」只渲染，不发送。

种子报文：

| 编码 | 方法与路径 | Body |
|------|------------|------|
| CRM | `POST {base}/contacts/update` | `{"event":"{{eventType}}","contact":{{payload}}}` |
| AD | `POST {base}/conversions` | `{"conversion_event":"{{eventType}}","data":{{payload}}}` |
| INVENTORY | `POST {base}/stock/adjust` | `{"event_type":"{{eventType}}","items":{{payload}}}` |

`application.yml` 里的 `notify-hub.channels` 只在供应商表为空时作为这三条的根地址，并用于改写上面三个旧端口。当前值为：

```yaml
notify-hub:
  channels:
    crm:
      base-url: http://127.0.0.1:8080/mock/crm
    ad:
      base-url: http://127.0.0.1:8080/mock/ad
    inventory:
      base-url: http://127.0.0.1:8080/mock/inventory
```

要投到真实系统，在供应商页面改地址和报文。

## 核心机制

| 机制 | 说明 |
|------|------|
| 投递语义 | At Least Once |
| 持久化 | 请求先写入 `notification_task` |
| 异步投递 | 调度器拉取 `PENDING` 和到点的 `FAILED` |
| 成功判定 | HTTP 2xx 为成功，其它状态码和网络异常为失败 |
| 重试 | 当前配置为 1 分钟一次，共 5 次；次数用尽后为 `DEAD` |
| 死信 | 详情页可把 `DEAD` 放回队列 |
| 幂等 | `request_id` 唯一索引 |

任务状态：`PENDING`（待投递）→ `SUCCESS`（成功），失败后为 `FAILED`（失败重试），超过次数为 `DEAD`（死信）。

## 配置项

| 配置 | 当前值 | 说明 |
|------|--------|------|
| `notify-hub.dispatcher.interval-ms` | 10000 | 两轮调度之间的间隔（毫秒） |
| `notify-hub.dispatcher.batch-size` | 50 | 每批处理任务数 |
| `notify-hub.retry-intervals-minutes` | 1,1,1,1,1 | 每次失败后的等待分钟数，列表长度即最大重试次数 |
| `notify-hub.channels.*.base-url` | 本机 `/mock/...` | 仅用于空表种子和旧端口改写 |

未配置重试间隔时，代码默认是 `1,5,30,60,360`。

## 运行测试

```bash
mvn test
```

需 JDK 21。与当前运行路径直接相关的测试：

| 测试类 | 覆盖点 |
|--------|--------|
| `VendorTemplateRendererTest` | 占位符、请求头合并、非法 JSON |
| `VendorSeederTest` | 空表写入三条供应商；只改写 `localhost:18081/18082/18083` |
| `NotificationDispatcherTest` | 发送成功、失败、重试、死信 |
| `NotificationServiceTest` | 落库、requestId 幂等、未知或停用的供应商 |
| `OpsPageTest` | 供应商、任务、手工提交页面 |
| `LocalMockControllerTest` | `POST /mock/crm/contacts/update` 返回 200 |

## 常见问题

**Q: 启动报数据库连接失败？**  
确认 MySQL 已启动，数据库 `notify_hub` 已创建，用户名密码与 `application.yml` 一致。

**Q: 任务一直待投递或失败重试？**  
看任务详情里的最近错误，以及该供应商页面上的地址是否可达。本机模拟要指向 `http://127.0.0.1:8080/mock/...`。调度间隔默认 10 秒，详情页会自动刷新。

**Q: 改了 yml 里的 base-url，投递地址没变？**  
投递读的是供应商表。yml 只在表为空时写入种子，并在启动时改写仍指向 `localhost:18081/18082/18083` 的旧地址。

**Q: 生产环境建议？**  
将 `spring.jpa.hibernate.ddl-auto` 改为 `validate`，使用 Flyway/Liquibase 管理表结构；把重试间隔改回更长的退避，并换成真实供应商地址。
