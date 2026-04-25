# online-exam 中间件部署

本目录提供一套面向 CentOS/Linux 单机的部署资产，适合当前开发测试和后续同机部署场景。

包含的容器服务：

- MySQL
- Redis
- RabbitMQ
- Nacos

`Sentinel` 不通过 Docker Compose 部署，改为 jar 包方式单独运行，本文后半部分给出示例。

当前推荐边界：

- Docker 优先承接 MySQL、Redis、RabbitMQ、Nacos 这类中间件
- Java / Node 微服务继续走宿主机启动，和仓库内现有脚本保持一致

## 目录说明

- `docker-compose.yaml`: 中间件编排文件
- `.env.example`: 环境变量模板
- `../sql/mysql.sql`: MySQL 首次启动时导入的初始化脚本

## 设计约定

- 默认只绑定到 `127.0.0.1`
- 不直接把 MySQL、Redis、RabbitMQ、Nacos 暴露到公网 `47.105.121.232`
- 开发环境建议通过 SSH 隧道访问
- 后续如果应用部署到同一台服务器，应用直接连接 `127.0.0.1` 即可

## 快速启动

1. 复制环境变量模板：

```bash
cp .env.example .env
```

2. 修改 `.env` 中的密码和令牌，至少检查以下变量：

- `MYSQL_ROOT_PASSWORD`
- `REDIS_PASSWORD`
- `RABBITMQ_DEFAULT_PASS`
- `NACOS_AUTH_TOKEN`
- `NACOS_AUTH_IDENTITY_KEY`
- `NACOS_AUTH_IDENTITY_VALUE`

3. 启动容器：

```bash
docker compose --env-file .env up -d
```

4. 查看运行状态：

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs -f
```

5. 校验编排文件：

```bash
docker compose --env-file .env config
```

## 服务端口

默认情况下，所有端口都只监听 `127.0.0.1`。

| 服务 | 容器端口 | 默认宿主机端口 | 用途 |
| --- | --- | --- | --- |
| MySQL | 3306 | 3306 | 业务数据库 |
| Redis | 6379 | 6379 | 缓存 |
| RabbitMQ | 5672 | 5672 | AMQP |
| RabbitMQ 管理台 | 15672 | 15672 | Web 控制台 |
| Nacos | 8848 | 8848 | 控制台和 HTTP API |
| Nacos gRPC | 9848 | 9848 | 客户端通信 |
| Nacos cluster/gRPC | 9849 | 9849 | Nacos 2.x 相关通信 |

如果你必须临时放开端口，不建议直接改成公网监听。更稳妥的方式是保留 `HOST_BIND_IP=127.0.0.1`，通过 SSH 隧道访问。

## SSH 隧道示例

如果开发机需要访问服务器 `47.105.121.232` 上的中间件，可以在本地执行：

```bash
ssh \
  -L 3306:127.0.0.1:3306 \
  -L 6379:127.0.0.1:6379 \
  -L 5672:127.0.0.1:5672 \
  -L 8848:127.0.0.1:8848 \
  -L 9848:127.0.0.1:9848 \
  -L 9849:127.0.0.1:9849 \
  -L 15672:127.0.0.1:15672 \
  root@47.105.121.232
```

建立隧道后，本地应用或浏览器继续访问：

- `127.0.0.1:3306`
- `127.0.0.1:6379`
- `127.0.0.1:5672`
- `127.0.0.1:8848`
- `127.0.0.1:15672`

## 初始化与持久化说明

- MySQL 会在第一次初始化数据目录时自动创建 `online_exam_db`
- `../sql/mysql.sql` 会被自动导入到该数据库
- 如果数据卷已经存在，MySQL 不会再次执行初始化脚本

如果应用报错 `Access denied for user 'root'@'172.x.x.x'`，这通常不是连错了数据库地址。
应用仍然可能是在连接 `127.0.0.1:3306`，只是 MySQL 容器内看到的客户端来源地址会变成 Docker 网桥地址。

为避免这个问题，当前编排默认会：

- 允许 `root` 从宿主机访问

如果你确认要重新初始化数据，先停止服务，再删除相关卷后重启。请只在测试环境这样做。

```bash
docker compose --env-file .env down
docker volume rm online-exam-infra_mysql_data
docker compose --env-file .env up -d
```

如果你已经有旧的数据卷，并且之前是按仅本地 `root` 账号初始化的，那么只改 Compose 文件不会自动修复现有授权。
这种情况下请二选一：

- 删除 MySQL 数据卷后重新初始化
- 手动在 MySQL 容器里执行授权语句，为 `root` 放开宿主机访问

## 当前项目配置衔接

当前 Spring Boot 服务已统一采用 `application.yaml + application-dev.yaml + application-local.properties` 三层配置方案。

当前本地联调口径如下：

- Java 服务通过 `application-local.properties` 指向 `127.0.0.1`
- Node 服务通过 `config/application-local.properties` 指向 `127.0.0.1`
- 前端通过 `web/.env.development` 指向本地网关与两个 WebSocket 服务

建议调整方式：

- 生产同机部署：继续使用 `127.0.0.1`
- 开发远程联调：先建立 SSH 隧道，再使用 `127.0.0.1`
- 不建议把中间件连接地址直接写成公网 `47.105.121.232`

如果要联调导出文件分发链路，以下服务需要把 `EXAM_RESOURCE_STORAGE_ROOT` 指向同一个本地目录或同一个挂载卷：

- `exam-question`
- `exam-paper`
- `exam-score`
- `exam-system`
- `exam-resource`

## 微服务端口

当前仓库内各微服务默认端口如下：

- `exam-gateway`: `8080`
- `exam-account`: `8081`
- `exam-class`: `8082`
- `exam-question`: `8083`
- `exam-paper`: `8084`
- `exam-system`: `8085`
- `exam-core`: `8086`
- `exam-score`: `8087`（已落地）
- `exam-issue-core`: `8088`（已落地）
- `exam-resource`: `8089`（已落地）
- `exam-realtime`: `8090`（已落地，Node.js + Fastify）
- `exam-issue-notify`: `8091`（已落地，Node.js + Fastify）

说明：

- `exam-system` 已调整为 `8085`，用于避免本地和 `exam-question` 的 `8083` 端口冲突
- `exam-paper` 使用 `8084`，作为试卷服务固定联调端口
- `exam-core` 使用 `8086`，用于避免和 `exam-system` 的 `8085` 端口冲突
- `exam-realtime` 使用 `8090`，通过 Nacos 注册为 `exam-realtime`，网关路由前缀为 `/api/exam/realtime/**`
- 网关按服务名路由，端口调整不会影响 `lb://exam-system` 这类服务发现路由写法

## exam-realtime 启动说明

`exam-realtime` 为独立 Node.js 微服务，目录位于 `apis/exam-realtime`。
完整服务边界、配置和联调口径见 `docs/architecture/exam-realtime技术栈与首批落地方案.md`。

- 安装依赖：`npm install`
- 启动开发服务：`npm run dev`
- 构建检查：`npm run build`
- 运行测试：`npm run test:run`

本地默认依赖：

- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- Nacos：`127.0.0.1:8848`
- `exam-core`：`http://127.0.0.1:8086`

## exam-issue-notify 启动说明

`exam-issue-notify` 为独立 Node.js 微服务，目录位于 `apis/exam-issue-notify`。
完整服务边界、配置和联调口径见 `docs/architecture/exam-issue-notify技术栈与首批落地方案.md`。

- 安装依赖：`npm install`
- 启动开发服务：`npm run dev`
- 构建检查：`npm run build`
- 运行测试：`npm run test:run`

本地默认依赖：

- MySQL：`127.0.0.1:3306`
- Nacos：`127.0.0.1:8848`
- 前端问题通知地址：`VITE_ISSUE_WS_BASE_URL=http://127.0.0.1:8091`

## web 真实联调说明

`web` 当前已支持 mock 与真实后端双模式。

- 默认本地开发配置位于 `web/.env.development`
- 环境变量模板位于 `web/.env.example`
- 测试环境固定使用 `web/.env.test`

关键变量说明：

- `VITE_API_BASE_URL`
  - 默认指向 `http://127.0.0.1:8080`
- `VITE_WS_BASE_URL`
  - 默认指向 `http://127.0.0.1:8090`
- `VITE_ISSUE_WS_BASE_URL`
  - 默认指向 `http://127.0.0.1:8091`
- `VITE_USE_MOCK`
  - 当前改为显式开关，只有设置为 `true` 时才走 mock

推荐联调方式：

1. 启动 MySQL、Redis、Nacos
2. 确认数据库已同步到最新 `docs/sql/mysql.sql`
3. 启动 `exam-account`、`exam-gateway`
4. 启动 `exam-core`、`exam-issue-core`
5. 启动 `exam-realtime`、`exam-issue-notify`
6. 在 `web` 目录执行 `npm run dev`

当前真实联调至少需要以下表已经存在，否则虽然服务能启动，但网关转发到真实链路后会直接返回缺表错误：

- `exam_instance`
- `exam_instance_student`
- `exam_score_record`
- `exam_score_detail`
- `exam_issue_record`
- `exam_issue_process_log`
- `exam_realtime_abnormal_record`

当前仓库的 MySQL 初始化脚本已统一收口为：

- 共 `36` 张表
- `0` 个外键
- 仅保留主键、唯一键和普通索引，遵循当前“不要使用外键”的约束

如果本地库还停留在较早阶段，常见现象是：

- `/api/exam/realtime/session` 返回 `Table 'online_exam_db.exam_instance' doesn't exist`
- `/api/issue/notify` 返回 `Table 'online_exam_db.exam_issue_record' doesn't exist`

## 一键启动与停止

如果你想一次性拉起前端和当前全部后端服务，可以直接使用 `docs/deploy` 里的脚本：

- 启动全部服务：

```powershell
pwsh -File .\docs\deploy\start-all-local.ps1 -Rebuild
```

- 启动全部服务并顺带拉起 SSH 隧道：

```powershell
pwsh -File .\docs\deploy\start-all-local.ps1 -StartTunnel -Rebuild
```

- 停止本次脚本拉起的全部服务：

```powershell
pwsh -File .\docs\deploy\stop-all-local.ps1
```

脚本行为说明：

- 启动顺序为：全部 Java 服务 -> `exam-realtime` -> `exam-issue-notify` -> `web`
- 传入 `-StartTunnel` 时，会先调用 `docs/deploy/start-tunnel.bat`，并等待 `3306/6379/8848` 就绪后再启动应用
- Java 服务会统一执行一次 Maven 打包，然后以 `--spring.profiles.active=dev,local` 方式启动
- Node 服务在缺少 `node_modules` 或构建产物时会自动安装依赖并构建
- 日志统一写入 `.runtime-logs`
- 进程记录统一写入 `.runtime-logs/local-dev-stack.processes.json`
- 如果端口已被占用，脚本会直接报错并提示占用进程，不会擅自杀进程

## Git 服务器部署

如果后续准备把后端统一迁到服务器运行，推荐使用 Git 主路径，而不是目录挂载或未提交文件直传。

新增脚本：

- 本地触发部署：`docs/deploy/deploy-server.ps1`
- 服务器接入检查：`docs/deploy/check-server-access.ps1`
- 服务器定向重启：`scripts/deploy/server-redeploy.sh`
- 本地只跑前端：`docs/deploy/start-web-local.ps1`

常用命令：

```powershell
pwsh -File .\docs\deploy\deploy-server.ps1 -Services exam-account,exam-gateway
pwsh -File .\docs\deploy\deploy-server.ps1 -All
pwsh -File .\docs\deploy\check-server-access.ps1
pwsh -File .\docs\deploy\start-web-local.ps1 -ApiBaseUrl "http://你的服务器地址:8080" -WsBaseUrl "http://你的服务器地址:8090" -IssueWsBaseUrl "http://你的服务器地址:8091"
```

详细步骤见：

- `docs/deploy/Git主路径服务器部署手册.md`
- `docs/deploy/服务器首轮接入检查清单.md`

仓库已补一份联调冒烟脚本，可用于快速判断当前是“服务没起”“鉴权有误”还是“数据库缺表”：

```powershell
pwsh -File .\scripts\live-integration-smoke.ps1
```

如果要重复验证考试答题主链，建议配套使用：

```powershell
pwsh -File .\scripts\reset-live-integration-state.ps1
```

如果 `exam-common` 变更后需要稳定重启 `exam-issue-core`，建议优先使用：

```powershell
pwsh -File .\scripts\start-issue-core.ps1 -Rebuild
```

如果你想把“联调状态重置 + 冒烟 + 必要时补拉 issue-core”合并成一步，建议直接使用：

```powershell
pwsh -File .\scripts\prepare-live-integration.ps1 -EnsureIssueCore -EnsureRealtime -EnsureIssueNotify
```

如果要直接验收两条真实主链，可继续使用：

```powershell
pwsh -File .\scripts\verify-live-exam-flow.ps1 -PrepareState
pwsh -File .\scripts\verify-live-issue-flow.ps1
```

如果想一条命令完成“环境准备 + 两条主链验收 + 最终摘要”，建议直接使用：

```powershell
pwsh -File .\scripts\verify-live-integration.ps1 -EnsureIssueCore -EnsureRealtime -EnsureIssueNotify
```

如果需要按“环境前置 -> 启动顺序 -> 前端配置 -> 主链验收 -> 问题通知验收 -> 排障”完整推进真实联调，请直接使用独立主文档：

- `docs/deploy/前后端真实联调操作手册.md`

约定：

- 本 README 继续负责中间件、端口、SSH 隧道和部署入口
- 联调主文档负责前后端真实联调步骤、验收链路、当前限制和排障顺序
- 后续若联调顺序或验收口径调整，优先更新联调主文档，再决定是否同步摘要到这里

## 最小联调种子

如果数据库结构已经同步完成，但业务表还是空库，那么 `realtime/session` 一类接口仍然会因为“考试不存在”或“未分发”而无法走通。
仓库已补一份最小联调种子脚本：

- `scripts/sql/live-integration-seed.sql`

这份脚本会补齐一套最小闭环样例：

- `Java 1班` 班级
- 一名教师 `teacher_li`
- 一名学生 `student_wang`
- 一张已发布试卷
- 一场进行中的考试
- 一条待提交成绩记录
- 两条成绩明细

推荐顺序：

1. 先执行 `docs/sql/mysql.sql`，确保 36 张表都已经建齐且没有外键。
2. 再执行 `scripts/sql/live-integration-seed.sql`，补最小业务样例。
3. 最后运行 `pwsh -File .\scripts\live-integration-smoke.ps1` 做冒烟确认。

说明：

- 种子脚本只覆盖固定联调样例 ID，不会清空全库。
- 脚本默认依赖当前初始化账号主键：教师 `3`、学生 `4`。
- 执行种子后，联调账号 `admin/manager/teacher_li/student_wang/auditor_chen/operator_zhao` 的默认密码会统一重置为 `123456`。
- 手机验证码发送仍依赖阿里云短信 API；若因余额不足失败，当前不作为真实联调阻塞项。
- 如果你的 `sys_user` 主键和仓库默认初始化值不一致，先按实际用户 ID 调整脚本再执行。

脚本会检查：

- 基础端口与业务端口是否监听
- `realtime/issue` 联调所需关键表是否存在
- 网关 `401`、`403`、真实路由转发是否符合预期

建议同时准备以下前端环境变量：

- `VITE_API_BASE_URL=http://127.0.0.1:8080`
- `VITE_WS_BASE_URL=http://127.0.0.1:8090`
- `VITE_ISSUE_WS_BASE_URL=http://127.0.0.1:8091`
- `VITE_USE_MOCK=false`

额外说明：

- 前端真实登录成功后会继续调用 `/api/system/permission/query` 初始化菜单、路由和按钮权限。
- 因此页面级真实联调时，除了 `exam-account`、`exam-core`、`exam-issue-core`、`exam-realtime`、`exam-issue-notify`，还必须保证 `exam-system` 已启动并可被网关转发。

## 服务访问建议

- MySQL: 使用 root 或额外创建业务账号访问
- Redis: 连接时需要携带密码
- RabbitMQ 管理台: `http://127.0.0.1:15672`
- Nacos 控制台: `http://127.0.0.1:8848/nacos`

说明：

- RabbitMQ 账号密码来自 `.env`
- Nacos 开启了鉴权，控制台默认建议使用 `nacos/<你在 .env 中配置的密码>`
- 如果 Nacos 首次登录后可修改密码，建议尽快修改

## Sentinel jar 部署

Sentinel Dashboard 官方文档提供的是 jar 包启动方式，适合单独部署，不强制放进 Compose。

### 启动示例

先把 `sentinel-dashboard.jar` 上传到服务器，例如放在 `/opt/sentinel/`：

```bash
mkdir -p /opt/sentinel
cd /opt/sentinel
java \
  -Dserver.port=8858 \
  -Dcsp.sentinel.dashboard.server=127.0.0.1:8858 \
  -Dproject.name=sentinel-dashboard \
  -jar sentinel-dashboard.jar
```

建议同样只监听本机，再通过 SSH 隧道访问：

```bash
ssh -L 8858:127.0.0.1:8858 your-user@47.105.121.232
```

然后本地浏览器打开：

```text
http://127.0.0.1:8858
```

### systemd 示例

可以把 Sentinel 注册为系统服务：

```ini
[Unit]
Description=Sentinel Dashboard
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/sentinel
ExecStart=/usr/bin/java -Dserver.port=8858 -Dcsp.sentinel.dashboard.server=127.0.0.1:8858 -Dproject.name=sentinel-dashboard -jar /opt/sentinel/sentinel-dashboard.jar
Restart=always
RestartSec=5
User=root

[Install]
WantedBy=multi-user.target
```

保存为 `/etc/systemd/system/sentinel-dashboard.service` 后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable sentinel-dashboard
sudo systemctl start sentinel-dashboard
sudo systemctl status sentinel-dashboard
```

## 后续建议

- 如果后续要正式生产化，可以把 Nacos 从内置存储切换到外部 MySQL
- 如果后续要容器化 Java 应用，可以让应用和中间件加入同一 Docker 网络，直接通过服务名访问
- 如果需要公网控制台访问，优先使用反向代理、IP 白名单和 HTTPS，而不是直接暴露原始端口