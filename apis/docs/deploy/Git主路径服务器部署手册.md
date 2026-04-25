# Git 主路径服务器部署手册

本文用于收口 `online-exam` 的“本地开发 + Git 推送 + 服务器 git pull + 定向重启”流程。

适用范围：

- 本地继续开发 `D:/CodeHome/online-exam/apis`
- 服务器运行后端微服务
- 本地仅运行 `web` 或只负责调试

## 1. 设计原则

- 服务器代码唯一真源为 Git 仓库，不再用目录挂载或未提交文件直传
- 本地改动只有在 `commit + push` 后，才允许进入服务器
- 服务器部署工作区禁止人工直接修改业务代码
- 构建与重启按服务定向进行，避免每次全量拉起全部微服务

## 2. 服务器首次准备

建议目录：

```bash
mkdir -p /opt/online-exam
cd /opt/online-exam
git clone git@github.com:ShiauLo/online-exam.git apis
mkdir -p runtime-logs runtime-pids
```

建议运行结果：

- Git 仓库根目录：`/opt/online-exam/apis`
- 工程目录：`/opt/online-exam/apis/apis`
- 日志目录：`/opt/online-exam/runtime-logs`
- PID 目录：`/opt/online-exam/runtime-pids`

服务器需要预装：

- Git
- JDK 21+
- Node.js 24.14.1+
- npm
- bash

服务器运行时还应准备：

- `/opt/online-exam/.env`
- 该文件同时供 Docker Compose 与部署脚本读取
- 如果未单独填写 `EXAM_DB_PASSWORD`、`EXAM_REDIS_PASSWORD`，部署脚本会回退使用 `MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`
- `EXAM_NACOS_PASSWORD` 需要显式填写，避免继续落到仓库里的占位值

当前推荐边界：

- MySQL、Redis、RabbitMQ、Nacos 这类中间件优先使用 Docker
- Java / Node 微服务本轮继续使用宿主机直跑

原因是当前仓库已经围绕“`git pull + 定向构建 + nohup + pid/log`”做了脚本收口，
这时把应用再套进 Docker，会把构建、重启和排障链路额外复杂化。

Nacos 版本说明：

- 当前 Java 服务使用的是 Spring Cloud Alibaba `2025.0.0.0`
- 对应客户端为 Nacos `3.0.x`
- 因此服务器侧 Nacos 不应继续停留在 `2.0.3`，建议直接统一到 `3.0.3`

## 3. 配置 GitHub SSH Key

服务器生成部署密钥：

```bash
ssh-keygen -t ed25519 -C "online-exam-deploy" -f ~/.ssh/online_exam_deploy
```

追加到 `~/.ssh/config`：

```sshconfig
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/online_exam_deploy
  IdentitiesOnly yes
```

把公钥内容加入 GitHub：

```bash
cat ~/.ssh/online_exam_deploy.pub
```

可作为：

- 仓库 Deploy Key
- 或当前 GitHub 账号的 SSH Key

验证：

```bash
ssh -T git@github.com
```

如果输出包含 `successfully authenticated`，说明 GitHub SSH 已打通。
如果仍然是 `Permission denied (publickey)`，说明公钥还没录入 GitHub，不能继续 `git clone` 或 `git pull`。

## 4. 检查服务器仓库 origin

进入服务器仓库：

```bash
cd /opt/online-exam/apis
git remote -v
```

必须是 SSH 形式：

```text
git@github.com:ShiauLo/online-exam.git
```

如果还是 HTTPS，改成：

```bash
git remote set-url origin git@github.com:ShiauLo/online-exam.git
```

## 5. 服务器部署脚本

仓库内新增脚本：

- `scripts/deploy/server-redeploy.sh`
- `scripts/deploy/start-all-server.sh`
- `scripts/deploy/stop-all-server.sh`
- `scripts/deploy/status-all-server.sh`

支持：

```bash
bash /opt/online-exam/apis/apis/scripts/deploy/server-redeploy.sh --repo /opt/online-exam/apis/apis --all
bash /opt/online-exam/apis/apis/scripts/deploy/server-redeploy.sh --repo /opt/online-exam/apis/apis --services exam-account,exam-gateway
bash /opt/online-exam/apis/apis/scripts/deploy/server-redeploy.sh --repo /opt/online-exam/apis/apis --runtime-root /opt/online-exam --env-file /opt/online-exam/.env --services exam-account,exam-gateway
```

脚本行为：

- 检查服务器工作区是否干净
- `git fetch origin`
- `git checkout main`
- `git pull --ff-only origin main`
- 自动加载 `/opt/online-exam/.env`，并把其中的 `EXAM_*` / 中间件密码注入服务启动参数
- 对目标服务执行构建与重启
- Java 构建默认使用更保守的 Maven 内存参数，并跳过测试与 Asciidoctor 文档生成
- 日志输出到 `/opt/online-exam/runtime-logs`
- PID 输出到 `/opt/online-exam/runtime-pids`

服务器总控脚本：

```bash
bash /opt/online-exam/apis/apis/scripts/deploy/start-all-server.sh --runtime-root /opt/online-exam --env-file /opt/online-exam/.env
bash /opt/online-exam/apis/apis/scripts/deploy/status-all-server.sh --runtime-root /opt/online-exam
bash /opt/online-exam/apis/apis/scripts/deploy/stop-all-server.sh --runtime-root /opt/online-exam
```

说明：

- `start-all-server.sh` 对外是一条命令，对内按“基础服务 -> 业务服务 -> 边缘服务”分组启动
- 同一分组内会并行启动，不会把全部微服务在同一秒无差别拉起
- 启动前会先检查 `3306/6379/8848` 是否可用
- 该脚本默认只启动已有构建产物；如果缺少 jar 或 Node 构建结果，先执行 `server-redeploy.sh`

## 6. 本地日常使用

先提交并推送：

```powershell
git add .
git commit -m "xxx"
git push origin main
```

然后执行本地总控脚本：

```powershell
pwsh -File .\docs\deploy\deploy-server.ps1 -Services exam-account,exam-gateway
pwsh -File .\docs\deploy\deploy-server.ps1 -All
```

说明：

- 脚本只允许从 `main` 分支部署
- 本地有未提交改动时会直接失败
- 本地落后远端或与远端分叉时会直接失败
- 本地领先远端时会自动 `git push origin main`

## 7. 定向重启规则

推荐使用以下规则选择部署范围：

- 修改 `exam-account/**`：部署 `exam-account`
- 修改 `exam-realtime/**`：部署 `exam-realtime`
- 修改 `exam-issue-notify/**`：部署 `exam-issue-notify`
- 修改 `exam-common/**`：部署全部 Java 微服务
- 修改根 `pom.xml`：部署全部 Java 微服务
- 只改 `docs/**`：默认不部署
- 只改 `web/**`：默认不触发服务器后端部署

如果不确定，直接使用：

```powershell
pwsh -File .\docs\deploy\deploy-server.ps1 -All
```

## 8. 本地只跑前端

如果后端已经在服务器运行，本地只需要启动前端：

```powershell
pwsh -File .\docs\deploy\start-web-local.ps1 `
  -ApiBaseUrl "http://你的服务器地址:8080" `
  -WsBaseUrl "http://你的服务器地址:8090" `
  -IssueWsBaseUrl "http://你的服务器地址:8091"
```

如果服务器后端只监听本机地址，请先自行建立 SSH 隧道，把 `8080/8090/8091` 转到本地后再启动前端。

## 9. 常见排查

### GitHub 拉取失败

优先检查：

- `ssh -T git@github.com` 是否通过
- `origin` 是否仍是 HTTPS
- 私钥是否被正确加载

### 服务器工作区脏

脚本会拒绝继续执行。先在服务器执行：

```bash
cd /opt/online-exam/apis
git status --short
```

确认脏文件来源，再手工清理。

### 构建失败

优先看：

- Maven 输出
- npm build 输出
- 对应服务日志文件

### 服务没起来

优先看：

- `/opt/online-exam/runtime-logs/<service>.out.log`
- `/opt/online-exam/runtime-logs/<service>.err.log`
- `/opt/online-exam/runtime-pids/<service>.pid`

### 网关正常但页面 503

优先确认：

- 服务是否真的监听对应端口
- 服务是否成功注册到 Nacos
- 网关是否能通过服务名发现下游实例
