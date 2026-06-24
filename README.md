# Notify Hub

统一通知中枢 MVP：业务系统通过 HTTP 提交通知，系统落库后异步投递至 CRM / 广告 / 库存等外部 API，支持幂等、重试与死信。

详细设计见 [设计说明.md](设计说明.md)。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 构建 | Maven 3.9+ |
| 框架 | Spring Boot 3.3 |
| 持久化 | Spring Data JPA + MySQL 8 |
| HTTP 客户端 | JDK `HttpClient` |
| 调度 | Spring `@Scheduled`（本地消息表轮询投递） |

## 项目结构

```
src/main/java/com/notifyhub/
├── api/              # REST 接入层
├── service/          # 通知提交与幂等
├── dispatcher/       # 异步投递与重试
├── acl/              # 防腐层：Converter + Channel
├── domain/           # 领域模型
├── entity/           # JPA 实体
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

### 2. 修改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/notify_hub?...
    username: your_user
    password: your_password
```

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

### 4. 提交通知

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

`targetSystem` 可选值：`CRM`、`AD`、`INVENTORY`。

相同 `requestId` 重复提交会返回已有任务（幂等）。

### 5. 外部渠道地址

在 `application.yml` 中配置各供应商 base URL（MVP 默认占位端口）：

```yaml
notify-hub:
  channels:
    crm:
      base-url: http://localhost:18081
    ad:
      base-url: http://localhost:18082
    inventory:
      base-url: http://localhost:18083
```

本地可用 [httpbin](https://httpbin.org) 或 mock 服务验证投递：

```yaml
notify-hub:
  channels:
    crm:
      base-url: https://httpbin.org
```

CRM 转换后会请求 `POST {base-url}/contacts/update`。

## 核心机制

| 机制 | 说明 |
|------|------|
| 投递语义 | At Least Once |
| 持久化 | 请求先写入 `notification_task` 表 |
| 异步投递 | 调度器每 10s 拉取 `PENDING` 任务 |
| 重试退避 | 1 / 5 / 30 / 60 / 360 分钟，共 5 次 |
| 死信 | 超过最大重试后状态变为 `DEAD` |
| 幂等 | `request_id` 唯一索引 |

任务状态流转：`PENDING` → `SUCCESS` / `FAILED`（重试中）→ `DEAD`。

## 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `notify-hub.dispatcher.interval-ms` | 10000 | 调度间隔（毫秒） |
| `notify-hub.dispatcher.batch-size` | 50 | 每批处理任务数 |
| `notify-hub.retry-intervals-minutes` | 1,5,30,60,360 | 重试间隔（分钟） |

## 运行测试

```bash
mvn test
```

测试覆盖：

| 测试类 | 覆盖点 |
|--------|--------|
| `NotificationConverterTest` | CRM / AD / INVENTORY 三渠道 URL、Body 字段、自定义 Header 合并 |
| `ConverterFactoryTest` | 工厂按渠道路由到对应 Converter |
| `NotificationDispatcherTest` | 模拟 Channel 发送成功/失败/重试/死信，校验各渠道请求 URL |
| `NotificationServiceTest` | 各渠道新任务落库、requestId 幂等、headers 序列化 |

需 JDK 21 方可编译执行。

## 常见问题

**Q: 启动报数据库连接失败？**  
确认 MySQL 已启动，数据库 `notify_hub` 已创建，用户名密码与 `application.yml` 一致。

**Q: 任务一直 PENDING？**  
检查 `notify-hub.channels.*.base-url` 是否可达；查看日志中的投递失败与重试信息。

**Q: 生产环境建议？**  
将 `spring.jpa.hibernate.ddl-auto` 改为 `validate`，使用 Flyway/Liquibase 管理表结构；按需开启连接池与监控。
