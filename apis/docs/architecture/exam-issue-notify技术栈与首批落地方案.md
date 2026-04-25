# exam-issue-notify 技术栈与首批落地方案

> 历史说明：当前运行拓扑已把 `exam-issue-notify` 并入 `exam-realtime`。本文档保留为历史方案记录；最新部署、端口和联调入口以 `docs/deploy/README.md` 与 `docs/deploy/前后端真实联调操作手册.md` 为准。

## 文档版本

- 版本：V1.0
- 更新时间：2026-04-26
- 维护人：Codex
- 文档定位：作为 `exam-issue-notify` 的历史收口主文档，记录问题通知能力的独立方案边界、技术栈、接口与事件设计。

## 1. 服务定位

- `exam-issue-notify` 是问题申报链路的实时通知服务，负责把 `exam-issue-core` 已写入数据库的问题创建和处理进度变化推送给前端。
- `exam-issue-notify` 不承载问题主流程，不修改 `exam_issue_record` 与 `exam_issue_process_log`，只读取已有表并做通知分发。
- 本轮不新增数据库表，不引入 MQ，不调用 `exam-issue-core` 远程接口，首版采用服务内轮询 MySQL 的方式完成最小闭环。

## 2. 固定边界

- HTTP 统一走网关 `/api/issue/notify/**`。
- WebSocket 首版继续由前端直连 `exam-issue-notify`，建议单独使用 `VITE_ISSUE_WS_BASE_URL`。
- 问题创建通知 `issueNotify` 只负责新问题到达提示。
- 处理进度通知 `processNotify` 只负责处理、转派、关闭等流转提示。
- `exam-issue-notify` 不接管问题查询、处理、转派、关闭和轨迹查询，这些仍归 `exam-issue-core`。

## 3. 技术栈与选型

- 服务框架：`Fastify`
- 语言：`TypeScript`
- 实时协议：`Socket.io`
- 包管理：`npm`
- 测试：`Vitest`
- 数据读取：`MySQL`
- 服务发现：`Nacos HTTP OpenAPI`

## 4. 首批接口与事件

### 4.1 HTTP

- `POST /api/issue/notify`
  - 请求结构：`{ cursor?, limit? }`
  - 返回结构：`{ scope, cursor, notifications }`
  - 用途：为当前登录用户拉取最近一批可见通知，便于调试、补拉和恢复。
  - `notifications[].issueId` 当前按字符串返回，避免 Snowflake Long ID 在前端 JavaScript 环境发生精度丢失。

### 4.2 Socket

- 前端连接后发送 `subscribeIssue`
  - 请求结构：`{ scope: "issue", cursor? }`
- 服务端返回 `connected`
  - 返回结构：`{ scope, userId, cursor }`
- 服务端推送 `issueNotify`
- 服务端推送 `processNotify`

补充约束：

- `issueNotify.issueId` 与 `processNotify.issueId` 当前统一按字符串返回，不再以 JSON number 暴露超出 JS 安全整数范围的 Snowflake ID。

## 5. 通知可见性规则

- `issueNotify`
  - `BUSINESS`：推给管理员；审计员按只读口径可见
  - `EXAM`：推给对应班级教师；审计员按只读口径可见
  - `SYSTEM`：推给系统运维；审计员按只读口径可见
- `processNotify`
  - 推给申请人和当前处理人
  - 审计员按只读口径可见

## 6. 数据来源

- `exam_issue_record`
- `exam_issue_process_log`
- `exam_class`
- `exam_instance`
- `sys_user`

## 7. 配置与启动

- 配置文件三层结构：
  - `config/application.yaml`
  - `config/application-dev.yaml`
  - `config/application-local.properties`
- 默认端口：`8091`
- 默认服务名：`exam-issue-notify`
- 启动命令：
  - `npm install`
  - `npm run test:run`
  - `npm run build`
  - `npm run dev`

联调前置：

- MySQL 已启动并已存在 `exam_issue_record`、`exam_issue_process_log`
- Nacos 已启动
- 网关已接入 `/api/issue/notify/** -> lb://exam-issue-notify`
- 前端已配置 `VITE_ISSUE_WS_BASE_URL`

## 8. 当前冻结与后续增强

当前已冻结：

- 首版不新增通知持久化表
- 首版不引入 MQ、Redis Stream、Kafka、RabbitMQ
- 首版不做多实例去重协调
- 首版不向 `exam-issue-core` 反向回写任何状态

后续增强项：

- 已读状态与未读计数
- 更细粒度订阅范围与聚合推送
- 多实例横向扩展下的事件去重
- 基于消息队列的通知驱动链路

## 9. 关联文档

- 总表：`docs/architecture/A-B-C一体化实施与后续待办总表.md`
- 优先级文档：`docs/architecture/未来微服务推进优先级与冻结标准.md`
- 总接口文档：`docs/api/在线考试系统接口文档（V3.0 适配Spring Boot+Node.js）.md`
- 前端文档：`docs/api/在线考试系统前端设计文档.md`
- 部署文档：`docs/deploy/README.md`
