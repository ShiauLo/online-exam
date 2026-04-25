# exam-realtime 技术栈与首批落地方案

## 文档版本

- 版本：V1.1
- 更新时间：2026-04-22
- 维护人：Codex
- 文档定位：作为 `exam-realtime` 的单独收口主文档，统一承接技术栈、边界、配置、接口、数据落点、启动和联调说明。

## 1. 服务定位

- `exam-realtime` 是当前考试过程层的唯一服务入口，负责进入考试、会话恢复、自动保存、倒计时、切屏上报、异常留痕和超时触发交卷。
- `exam-realtime` 不接管考试主链规则，不自己写 `exam_score_record` 与 `exam_score_detail` 的正式提交结果。
- 正式交卷仍委托 `exam-core` 的 `/api/exam/core/submit`，以保持成绩初始化、提交校验、客观题自动判分和状态推进的单一来源。
- `report-abnormal` 不再委托 `exam-issue-core`，而是直接写最小异常记录表 `exam_realtime_abnormal_record`，避免把实时过程异常强行拉入问题流转服务。

## 2. 固定边界

- 过程归 `exam-realtime`，正式交卷落库仍走 `exam-core`。
- HTTP 统一走网关 `/api/exam/realtime/**`，WebSocket 首版继续由前端通过 `VITE_WS_BASE_URL` 直连。
- 首版按单实例闭环，不承诺多实例抢占协调、教师实时监考推送或消息总线扩展。
- 当前异常上报仅做实时过程留痕，不扩展为 `exam-issue-core` 的处理、转派、关闭模型。

## 3. 技术栈与选型理由

- 服务框架：`Fastify`
- 语言：`TypeScript`
- 实时协议：`Socket.io`
- 包管理：`npm`
- 测试：`Vitest`
- 缓存：`Redis`
- 数据读取：`MySQL`
- 服务发现：`Nacos HTTP OpenAPI`
- 下游调用：Node 24 原生 `fetch`

选择这套组合的原因如下：

- 前端已使用 `socket.io-client`，继续沿用可直接联调。
- 实时过程请求频率高、连接长、状态短，`Fastify + Socket.io` 更适合这一层。
- Redis 适合承载草稿答案、切屏次数、活跃连接和提交锁等短生命周期状态。
- `submit` 继续委托 `exam-core`，可避免把考试正式提交规则复制两份。

## 4. 通信、认证与依赖

### 4.1 HTTP

- 前端通过网关访问 `/api/exam/realtime/**`。
- 服务默认信任网关透传的 `Authorization`、`X-User-Id`、`X-Role-Id`、`X-Request-Id`。
- 为兼容本地直连调试，若请求未经过网关但携带 `Authorization`，服务允许回退到本地 JWT 解析。

### 4.2 WebSocket

- 前端继续直连 `VITE_WS_BASE_URL`。
- 直连场景没有网关透传头，因此连接时显式携带 `accessToken`。
- 服务端在握手阶段解析 token，再校验 `enterExam` 与 `reportScreen`。

### 4.3 基础依赖职责

- MySQL：读取考试、分发、题目与正式答卷快照；写入异常记录表。
- Redis：保存草稿答案、当前题号、切屏次数、活跃连接和提交锁。
- Nacos：提供 `exam-realtime` 注册、续约和注销。
- `exam-core`：承接正式交卷。

## 5. 配置与目录

### 5.1 配置文件

- `config/application.yaml`
  - 固定默认值：服务名 `exam-realtime`、端口 `8090`、激活 `dev,local`
- `config/application-dev.yaml`
  - 放环境占位：MySQL、Redis、Nacos、`exam-core` 地址
- `config/application-local.properties`
  - 放本地联调值

配置读取顺序固定为：

1. `application.yaml`
2. `application-dev.yaml`
3. `application-local.properties`
4. `process.env`

### 5.2 模块目录

- `src/app.ts`
- `src/server.ts`
- `src/routes`
- `src/socket`
- `src/repositories`
- `src/services`
- `src/clients`
- `src/tests`

## 6. 首批接口与事件闭环

### 6.1 HTTP 接口

- `POST /api/exam/realtime/session`
  - 校验考试存在、已开始、当前学生已分发、尚未正式提交。
  - 返回考试信息、题目列表、服务端剩余秒数、草稿答案和切屏次数。
- `POST /api/exam/realtime/save-progress`
  - 每次覆盖当前草稿答案与 `currentQId`，只落 Redis。
- `PUT /api/exam/realtime/submit`
  - 对外由 `exam-realtime` 承接，对内委托 `exam-core` 完成正式交卷。
- `POST /api/exam/realtime/report-abnormal`
  - 直接写 `exam_realtime_abnormal_record`，不扩成完整问题流转。

### 6.2 Socket 事件

- `enterExam`
  - 单考生单活跃连接，新连接顶掉旧连接。
  - 成功后先发 `connected`，再按秒推 `countdown`。
- `reportScreen`
  - 只同步 Redis 中的切屏次数，不额外触发问题流程。
- `connected`
  - 表示进入考试房间成功。
- `countdown`
  - 由服务端按秒推送剩余时长。

### 6.3 超时交卷

- 超时时机由 `exam-realtime` 负责触发。
- 正式落成绩表仍调用 `exam-core`。
- Redis 提交锁用于防止重复提交。

## 7. 数据落点

### 7.1 MySQL

- 新增表：`exam_realtime_abnormal_record`
- 固定字段：
  - `abnormal_id`
  - `exam_id`
  - `reporter_id`
  - `reporter_role_id`
  - `type`
  - `description`
  - `img_urls`
  - `screen_out_count`
  - `request_id`
  - `create_time`
  - `update_time`

### 7.2 Redis

- 单考生单考试粒度保存以下状态：
  - 草稿答案
  - `currentQId`
  - `lastSavedAt`
  - `screenOutCount`
  - `submitLock`
  - `activeSocketId`
- TTL 固定为考试结束后额外保留 `24h`。

## 8. 启动与联调

- 安装依赖：`npm install`
- 启动开发服务：`npm run dev`
- 构建检查：`npm run build`
- 运行测试：`npm run test:run`

联调前置：

- MySQL、Redis、Nacos 已启动
- `exam-core` 已启动并可访问 `http://127.0.0.1:8086`
- 前端 `VITE_WS_BASE_URL` 指向 `exam-realtime`
- 网关已配置 `/api/exam/realtime/** -> lb://exam-realtime`

推荐联调顺序：

1. `/session`
2. `enterExam`
3. `/save-progress`
4. `reportScreen`
5. `/submit`
6. `/report-abnormal`

## 9. 当前冻结与后续增强

当前已冻结：

- `exam-realtime` 是考试过程服务的唯一承接方。
- 正式交卷结果仍由 `exam-core` 负责写入正式成绩表。
- `report-abnormal` 当前只做最小异常留痕，不进入问题主流程。
- WebSocket 首版继续前端直连，不通过网关代理 Socket.io。

后续增强项：

- 多实例 Socket 协调
- 教师端实时监考广播
- MQ、WebSocket 集群、分布式任务调度
- 更细粒度实时策略和异常告警升级

## 10. 关联文档

- 总表：`docs/architecture/A-B-C一体化实施与后续待办总表.md`
- 优先级文档：`docs/architecture/未来微服务推进优先级与冻结标准.md`
- 总接口文档：`docs/api/在线考试系统接口文档（V3.0 适配Spring Boot+Node.js）.md`
- 前端文档：`docs/api/在线考试系统前端设计文档.md`
- 部署文档：`docs/deploy/README.md`
- SQL 文档：`docs/sql/mysql.sql`

## 11. 修订记录

| 版本 | 时间 | 内容 | 维护人 |
| --- | --- | --- | --- |
| V1.0 | 2026-04-22 | 初版落地 exam-realtime 技术栈、边界与首批闭环说明 | Codex |
| V1.1 | 2026-04-22 | 收口为独立主文档，补齐配置、依赖、数据落点、联调顺序和关联文档入口 | Codex |
